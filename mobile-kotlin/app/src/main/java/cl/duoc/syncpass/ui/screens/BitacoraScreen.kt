package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.BitacoraDto
import cl.duoc.syncpass.data.RespuestaBitacora
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.launch

@Composable
fun BitacoraScreen(repository: SyncPassRepository, tallerId: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var data by remember(tallerId) { mutableStateOf<BitacoraDto?>(null) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<RespuestaBitacora?>(null) }
    var status by remember { mutableStateOf("Cargando bitácora...") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(tallerId) {
        val token = AuthSession.accessToken
        if (token == null) { status = "Inicia sesión para consultar la bitácora."; return@LaunchedEffect }
        runCatching { repository.bitacora(tallerId, token) }
            .onSuccess { data = it; status = "" }
            .onFailure { status = "No se pudo cargar la bitácora. Comprueba la conexión o vuelve más tarde." }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Volver a talleres") }
        Text("Bitácora del taller", style = MaterialTheme.typography.headlineMedium)
        Text("Resumen y preguntas basados en el contenido publicado para este taller.")
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.error)
        data?.let { item ->
            Card { Column(Modifier.padding(16.dp)) {
                Text("Resumen de la sesión", style = MaterialTheme.typography.titleLarge)
                Text(item.resumen)
            } }
            OutlinedTextField(value = question, onValueChange = { question = it.take(300) },
                label = { Text("Pregunta sobre el taller") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val token = AuthSession.accessToken ?: return@Button
                busy = true; status = ""; answer = null
                scope.launch {
                    runCatching { repository.preguntarBitacora(tallerId, token, question.trim()) }
                        .onSuccess { answer = it }
                        .onFailure { status = "No se pudo consultar la bitácora. Comprueba la conexión." }
                    busy = false
                }
            }, enabled = question.isNotBlank() && !busy) { Text("Buscar respuesta") }
            answer?.let { result -> Card { Column(Modifier.padding(16.dp)) {
                Text(result.respuesta)
                if (result.fragmento > 0) Text("Fuente: fragmento ${result.fragmento}", style = MaterialTheme.typography.labelMedium)
            } } }
        }
    }
}
