package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import org.json.JSONObject

internal fun authError(error: Throwable): String = if (error is HttpException) {
    runCatching { JSONObject(error.response()?.errorBody()?.string().orEmpty()).optString("message") }
        .getOrNull()?.takeIf { it.isNotBlank() } ?: "No se pudo completar la solicitud (${error.code()})."
} else "No se pudo conectar con la API. Revisa la conexión e inténtalo otra vez."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(repository: SyncPassRepository, onNavigateBack: () -> Unit, onNavigateToRegister: () -> Unit, onLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Iniciar sesión") }, navigationIcon = {
        TextButton(onClick = onNavigateBack) { Text("←") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Accede a SyncPass", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(username, { username = it }, label = { Text("Usuario o correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            AuthSession.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                busy = true; error = null
                scope.launch {
                    runCatching { repository.login(username.trim(), password) }
                        .onSuccess { AuthSession.signedIn(it.accessToken, it.idToken); onLogin() }
                        .onFailure { error = authError(it) }
                    busy = false
                }
            }, enabled = !busy && username.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "Ingresando…" else "Iniciar sesión")
            }
            TextButton(onClick = onNavigateToRegister) { Text("Crear una cuenta") }
        }
    }
}
