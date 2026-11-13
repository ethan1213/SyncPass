package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.layout.AREA_TACTIL_MINIMA
import cl.duoc.syncpass.ui.syncPassViewModel

/** Encuesta de satisfaccion del taller. El guardado lo decide [SurveyViewModel]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyScreen(repository: SyncPassRepository, tallerId: Long, onNavigateBack: () -> Unit) {
    val vm = syncPassViewModel(repository) { SurveyViewModel(it) }
    val state by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Encuesta del taller") },
            navigationIcon = { TextButton(onClick = onNavigateBack) { Text("←") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Taller $tallerId · Calificación: ${state.rating}/5",
                style = MaterialTheme.typography.titleLarge)

            Slider(
                value = state.rating.toFloat(),
                onValueChange = { vm.onRating(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3
            )
            OutlinedTextField(
                value = state.comment,
                onValueChange = vm::onComment,
                label = { Text("Comentario opcional") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            if (state.message.isNotEmpty()) Text(state.message)
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.guardar(tallerId) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
            ) {
                Text("Guardar encuesta")
            }
        }
    }
}
