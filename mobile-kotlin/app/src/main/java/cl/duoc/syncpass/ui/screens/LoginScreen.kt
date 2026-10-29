package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.AuthSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onNavigateBack: () -> Unit, onNavigateToRegister: () -> Unit, onLogin: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Iniciar sesión") }, navigationIcon = {
        TextButton(onClick = onNavigateBack) { Text("←") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Accede de forma segura con Amazon Cognito", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            AuthSession.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesión") }
            TextButton(onClick = onNavigateToRegister) { Text("Crear una cuenta") }
        }
    }
}
