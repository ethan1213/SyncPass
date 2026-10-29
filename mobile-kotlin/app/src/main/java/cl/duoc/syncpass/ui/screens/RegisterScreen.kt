package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(repository: SyncPassRepository, onNavigateBack: () -> Unit, onNavigateToLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var awaitingCode by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Crear cuenta") }, navigationIcon = {
        TextButton(onClick = onNavigateBack) { Text("←") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(if (awaitingCode) "Confirma tu correo" else "Regístrate en SyncPass", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            if (!awaitingCode) {
                OutlinedTextField(username, { username = it }, label = { Text("Usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(nickname, { nickname = it }, label = { Text("Nombre visible") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Mínimo 8 caracteres; incluye mayúscula, minúscula, número y símbolo.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { repository.register(username.trim(), email.trim(), nickname.trim(), password) }
                            .onSuccess { result ->
                                password = ""
                                if (result.userConfirmed) onNavigateToLogin()
                                else { awaitingCode = true; message = "Enviamos un código a ${result.destination}." }
                            }.onFailure { error = authError(it) }
                        busy = false
                    }
                }, enabled = !busy && username.isNotBlank() && email.isNotBlank() && nickname.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Registrando…" else "Crear cuenta") }
            } else {
                OutlinedTextField(code, { code = it }, label = { Text("Código de verificación") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { repository.confirm(username.trim(), code.trim()) }
                            .onSuccess { onNavigateToLogin() }
                            .onFailure { error = authError(it) }
                        busy = false
                    }
                }, enabled = !busy && code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Confirmar correo") }
                TextButton(onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { repository.resend(username.trim()) }
                            .onSuccess { message = "Enviamos un nuevo código." }
                            .onFailure { error = authError(it) }
                        busy = false
                    }
                }, enabled = !busy) { Text("Reenviar código") }
            }
            message?.let { Text(it) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onNavigateToLogin) { Text("Ya tengo cuenta") }
        }
    }
}
