package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.PendingRecord
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyScreen(repository: SyncPassRepository, tallerId: Long, onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Encuesta del taller") }, navigationIcon = {
        TextButton(onClick = onNavigateBack) { Text("←") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Taller $tallerId · Calificación: $rating/5", style = MaterialTheme.typography.titleLarge)
            Slider(value = rating.toFloat(), onValueChange = { rating = it.toInt().coerceIn(1,5) }, valueRange = 1f..5f, steps = 3)
            OutlinedTextField(value = comment, onValueChange = { comment = it.take(2000) },
                label = { Text("Comentario opcional") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            if (message.isNotEmpty()) Text(message)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val owner = AuthSession.participantId
                if (owner == null) message = "Inicia sesión antes de responder."
                else if (tallerId <= 0) message = "Selecciona un taller."
                else scope.launch {
                    try {
                        repository.queue(PendingRecord(owner = owner, tipo = "encuesta", tallerId = tallerId,
                            rating = rating, comentario = comment, utilidad = "muy_util"))
                        message = "Encuesta guardada. Sincronízala desde Inicio."
                        comment = ""
                    } catch (e: Exception) { message = e.message ?: "No se pudo guardar." }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar encuesta") }
        }
    }
}
