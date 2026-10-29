package cl.duoc.syncpass

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import android.net.Uri
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
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val oauth by lazy { AuthorizationService(this) }
    private val repository by lazy { SyncPassRepository(applicationContext) }
    private val connectivity by lazy { getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { runOnUiThread { syncPending() } }
    }
    private var afterLogin: (() -> Unit)? = null
    private fun syncPending() {
        val owner = AuthSession.participantId ?: return
        val token = AuthSession.accessToken ?: return
        lifecycleScope.launch {
            runCatching { repository.sync(owner, token) }.onSuccess { if (it > 0) AuthSession.refreshed() }
        }
    }
    private val authResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val response = data?.let(AuthorizationResponse::fromIntent)
        val error = data?.let(AuthorizationException::fromIntent)
        if (response == null) {
            AuthSession.fail(error?.errorDescription ?: "No se completó el inicio de sesión.")
        } else {
            oauth.performTokenRequest(response.createTokenExchangeRequest()) { tokens, exchangeError ->
                if (tokens?.accessToken == null) AuthSession.fail(exchangeError?.errorDescription ?: "No se pudo obtener la sesión.")
                else {
                    AuthSession.signedIn(tokens.accessToken!!, tokens.idToken)
                    afterLogin?.invoke()
                    syncPending()
                }
            }
        }
    }

    private fun authenticate(onSuccess: () -> Unit) {
        afterLogin = onSuccess
        val domain = "https://us-east-12c9bbmkgd.auth.us-east-1.amazoncognito.com"
        val config = AuthorizationServiceConfiguration(
            Uri.parse("$domain/oauth2/authorize"), Uri.parse("$domain/oauth2/token"))
        val request = AuthorizationRequest.Builder(config, "677bhbj1qg892bmlk2pr1h6had",
            ResponseTypeValues.CODE, Uri.parse("syncpass://auth/callback"))
            .setScope("openid email").build()
        authResult.launch(oauth.getAuthorizationRequestIntent(request))
    }

    override fun onDestroy() {
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        oauth.dispose()
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
                    SyncPassNavigation(repository, ::authenticate)
                }
            }
        }
    }
}

@Composable
fun SyncPassNavigation(repository: SyncPassRepository, authenticate: (() -> Unit) -> Unit) {
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLogin = { authenticate {
                    // Evita que el login quede en la pila al volver atras.
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                } }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.popBackStack(Screen.Login.route, inclusive = false)
                },
                onRegisterSuccess = { authenticate { navController.popBackStack(Screen.Home.route, inclusive = false) } }
            )
        }
    }
}
