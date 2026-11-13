package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.layout.AREA_TACTIL_MINIMA
import cl.duoc.syncpass.ui.syncPassViewModel

/** Registro de participante en dos pasos, gobernado por [RegisterViewModel]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    repository: SyncPassRepository,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val vm = syncPassViewModel(repository) { RegisterViewModel(it) }
    val state by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Crear cuenta") },
            navigationIcon = { TextButton(onClick = onNavigateBack) { Text("←") } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Text(
                text = if (state.esperandoCodigo) "Confirma tu correo" else "Regístrate en SyncPass",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(20.dp))

            if (!state.esperandoCodigo) {
                DatosDeCuenta(state, vm, onNavigateToLogin)
            } else {
                ConfirmacionPorCodigo(state, vm, onNavigateToLogin)
            }

            state.message?.let { Text(it) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onNavigateToLogin) { Text("Ya tengo cuenta") }
        }
    }
}

@Composable
private fun DatosDeCuenta(state: RegisterUiState, vm: RegisterViewModel, onCuentaLista: () -> Unit) {
    OutlinedTextField(state.username, vm::onUsername, label = { Text("Usuario") },
        singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(state.email, vm::onEmail, label = { Text("Correo") },
        singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(state.nickname, vm::onNickname, label = { Text("Nombre visible") },
        singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(state.password, vm::onPassword, label = { Text("Contraseña") },
        visualTransformation = PasswordVisualTransformation(), singleLine = true,
        modifier = Modifier.fillMaxWidth())
    Text("Mínimo 8 caracteres; incluye mayúscula, minúscula, número y símbolo.",
        style = MaterialTheme.typography.bodySmall)

    Button(
        onClick = { vm.registrar(onCuentaLista) },
        enabled = state.puedeRegistrar,
        modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
    ) {
        Text(if (state.busy) "Registrando…" else "Crear cuenta")
    }
}

@Composable
private fun ConfirmacionPorCodigo(
    state: RegisterUiState,
    vm: RegisterViewModel,
    onNavigateToLogin: () -> Unit
) {
    OutlinedTextField(state.code, vm::onCode, label = { Text("Código de verificación") },
        singleLine = true, modifier = Modifier.fillMaxWidth())

    Button(
        onClick = { vm.confirmar(onNavigateToLogin) },
        enabled = state.puedeConfirmar,
        modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
    ) {
        Text("Confirmar correo")
    }
    TextButton(onClick = vm::reenviarCodigo, enabled = !state.busy) { Text("Reenviar código") }
}
