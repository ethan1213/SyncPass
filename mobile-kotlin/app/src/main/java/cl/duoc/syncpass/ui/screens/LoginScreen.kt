package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
    val state by vm.state.collectAsState()

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
                onClick = { vm.iniciarSesion(onLogin) },
                enabled = state.puedeEnviar,
                modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
            ) {
                Text(if (state.busy) "Ingresando…" else "Iniciar sesión")
            }
            TextButton(onClick = onNavigateToRegister) { Text("Crear una cuenta") }
        }
    }
}
