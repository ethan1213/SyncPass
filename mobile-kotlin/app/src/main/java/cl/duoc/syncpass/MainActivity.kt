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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.syncpass.ui.layout.LocalFormatoPantalla
import cl.duoc.syncpass.ui.layout.recordarFormatoPantalla
import cl.duoc.syncpass.ui.navigation.Screen
import cl.duoc.syncpass.ui.screens.HomeScreen
import cl.duoc.syncpass.ui.screens.EntryScreen
import cl.duoc.syncpass.ui.screens.EntryViewModel
import cl.duoc.syncpass.ui.syncPassViewModel
import cl.duoc.syncpass.ui.screens.GuestQrScreen
import cl.duoc.syncpass.ui.screens.GuestTallerScreen
import cl.duoc.syncpass.ui.screens.LoginScreen
import cl.duoc.syncpass.ui.screens.RegisterScreen
import cl.duoc.syncpass.ui.screens.ScannerScreen
import cl.duoc.syncpass.ui.screens.SurveyScreen
import cl.duoc.syncpass.ui.screens.BitacoraScreen
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
    // Un solo EntryViewModel para todo el flujo de entrada: la pantalla inicial,
    // el escaner del QR del taller y el detalle comparten el mismo estado.
    val entryVm = syncPassViewModel(repository) { EntryViewModel(it) }
    var selectedTaller by remember { mutableLongStateOf(0L) }
    var guestTallerId by remember { mutableStateOf<Long?>(null) }

    NavHost(
        navController = navController,
        startDestination = Screen.Entry.route
    ) {
        composable(Screen.Entry.route) {
            EntryScreen(
                vm = entryVm,
                onLogin = { navController.navigate(Screen.Login.route) },
                onContinue = { navController.navigate(Screen.Home.route) },
                onScanQr = { navController.navigate(Screen.GuestQr.route) },
                onGuestTaller = { id ->
                    guestTallerId = id
                    navController.navigate(Screen.GuestTaller.route) {
                        popUpTo(Screen.Entry.route) { inclusive = false }
                    }
                }
            )
        }
        composable(Screen.GuestQr.route) {
            GuestQrScreen(entryVm,
                onGuestTaller = { id ->
                    guestTallerId = id
                    navController.navigate(Screen.GuestTaller.route) {
                        popUpTo(Screen.Entry.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() })
        }
        composable(Screen.GuestTaller.route) {
            GuestTallerScreen(entryVm,
                onBack = { navController.popBackStack() },
                onLogin = { navController.navigate(Screen.Login.route) })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                guestTallerId = guestTallerId,
                onNavigateToScanner = { selectedTaller = it; navController.navigate(Screen.Scanner.route) },
                onNavigateToSurvey = { selectedTaller = it; navController.navigate(Screen.Survey.route) },
                onNavigateToBitacora = { selectedTaller = it; navController.navigate(Screen.Bitacora.route) },
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

        composable(Screen.Bitacora.route) {
            BitacoraScreen(repository, selectedTaller) { navController.popBackStack() }
        }

        composable(Screen.Login.route) {
            LoginScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLogin = {
                    val attendedTallerId = guestTallerId
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Entry.route) { inclusive = true }
                    }
                    if (attendedTallerId != null && entryVm.selectedTaller?.latitud == null)
                        AuthSession.accessToken?.let { entryVm.joinAccount(attendedTallerId, it) }
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
