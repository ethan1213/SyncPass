package cl.duoc.syncpass

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import cl.duoc.syncpass.ui.layout.LocalFormatoPantalla
import cl.duoc.syncpass.ui.layout.recordarFormatoPantalla
import cl.duoc.syncpass.ui.navigation.Screen
import cl.duoc.syncpass.ui.screens.HomeScreen
import cl.duoc.syncpass.ui.screens.LoginScreen
import cl.duoc.syncpass.ui.screens.RegisterScreen
import cl.duoc.syncpass.ui.screens.ScannerScreen
import cl.duoc.syncpass.ui.screens.SurveyScreen
import cl.duoc.syncpass.ui.theme.SyncPassTheme
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository by lazy { SyncPassRepository(applicationContext) }
    private val connectivity by lazy { getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { runOnUiThread { syncPending() } }
    }
    private fun syncPending() {
        val owner = AuthSession.participantId ?: return
        val token = AuthSession.accessToken ?: return
        lifecycleScope.launch {
            runCatching { repository.sync(owner, token) }.onSuccess { if (it > 0) AuthSession.refreshed() }
        }
    }
    override fun onDestroy() {
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        super.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectivity.registerDefaultNetworkCallback(networkCallback)
        setContent {
            // El formato se recalcula al rotar o en multiventana, de modo que la
            // misma app sirve en telefono y en tablet (caso §3.3).
            val formato = recordarFormatoPantalla(this)

            SyncPassTheme {
                CompositionLocalProvider(LocalFormatoPantalla provides formato) {
                    SyncPassNavigation(repository, ::syncPending)
                }
            }
        }
    }
}

@Composable
fun SyncPassNavigation(repository: SyncPassRepository, onAuthenticated: () -> Unit) {
    val navController = rememberNavController()
    var selectedTaller by remember { mutableLongStateOf(0L) }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                onNavigateToScanner = { selectedTaller = it; navController.navigate(Screen.Scanner.route) },
                onNavigateToSurvey = { selectedTaller = it; navController.navigate(Screen.Survey.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                repository = repository,
                tallerId = selectedTaller,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Survey.route) {
            SurveyScreen(
                repository = repository,
                tallerId = selectedTaller,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLogin = {
                    // Evita que el login quede en la pila al volver atras.
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                    onAuthenticated()
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.popBackStack(Screen.Login.route, inclusive = false)
                }
            )
        }
    }
}
