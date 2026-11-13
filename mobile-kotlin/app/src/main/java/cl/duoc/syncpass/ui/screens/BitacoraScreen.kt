package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.layout.AREA_TACTIL_MINIMA
import cl.duoc.syncpass.ui.syncPassViewModel

/** Resumen y preguntas de la bitacora publicada, servidos por [BitacoraViewModel]. */
@Composable
fun BitacoraScreen(repository: SyncPassRepository, tallerId: Long, onBack: () -> Unit) {
    val vm = syncPassViewModel(repository) { BitacoraViewModel(it) }
    val state by vm.state.collectAsState()

    LaunchedEffect(tallerId) { vm.cargar(tallerId) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("Volver a talleres") }
        Text("Bitácora del taller", style = MaterialTheme.typography.headlineMedium)
        Text("Resumen y preguntas basados en el contenido publicado para este taller.")

        if (state.status.isNotBlank()) Text(state.status, color = MaterialTheme.colorScheme.error)

        state.bitacora?.let { bitacora ->
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Resumen de la sesión", style = MaterialTheme.typography.titleLarge)
                    Text(bitacora.resumen)
                }
            }

            OutlinedTextField(
                value = state.question,
                onValueChange = vm::onQuestion,
                label = { Text("Pregunta sobre el taller") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { vm.preguntar(tallerId) },
                enabled = state.question.isNotBlank() && !state.busy,
                modifier = Modifier.heightIn(min = AREA_TACTIL_MINIMA)
            ) {
                Text("Buscar respuesta")
            }

            state.answer?.let { respuesta ->
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(respuesta.respuesta)
                        if (respuesta.fragmento > 0) Text(
                            text = "Fuente: fragmento ${respuesta.fragmento}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
