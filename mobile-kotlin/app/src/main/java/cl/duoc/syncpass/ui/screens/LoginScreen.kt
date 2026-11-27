package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.layout.AREA_TACTIL_MINIMA
import cl.duoc.syncpass.ui.syncPassViewModel

/**
 * Acceso con cuenta. La vista solo dibuja y avisa de los eventos: quien llama
 * a la API y guarda la sesion es [LoginViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    repository: SyncPassRepository,
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLogin: () -> Unit
) {
    val vm = syncPassViewModel(repository) { LoginViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val navigateAfterLogin by rememberUpdatedState(onLogin)
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.loginSuccess.collect { navigateAfterLogin() }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Iniciar sesión") },
            navigationIcon = { TextButton(onClick = onNavigateBack) { Text("←") } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Accede a SyncPass", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.username,
                onValueChange = vm::onUsername,
                label = { Text("Usuario o correo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = vm::onPassword,
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            AuthSession.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = vm::iniciarSesion,
                enabled = state.puedeEnviar,
                modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
            ) {
                Text(if (state.busy) "Ingresando…" else "Iniciar sesión")
            }
            TextButton(onClick = onNavigateToRegister) { Text("Crear una cuenta") }
        }
    }
}
