package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onNavigateBack: () -> Unit, onNavigateToLogin: () -> Unit, onRegisterSuccess: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Crear cuenta") }, navigationIcon = {
        TextButton(onClick = onNavigateBack) { Text("←") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("El registro y la verificación por correo se realizan en Cognito.",
                style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRegisterSuccess, modifier = Modifier.fillMaxWidth()) { Text("Abrir registro seguro") }
            TextButton(onClick = onNavigateToLogin) { Text("Ya tengo cuenta") }
        }
    }
}
