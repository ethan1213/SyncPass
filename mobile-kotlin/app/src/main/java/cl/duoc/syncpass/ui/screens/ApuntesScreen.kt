package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.syncPassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApuntesScreen(repository: SyncPassRepository, tallerId: Long, onBack: () -> Unit) {
    val vm = syncPassViewModel(repository) { ApuntesViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val palette = MaterialTheme.colorScheme
    LaunchedEffect(tallerId) { vm.cargar(tallerId) }
    Scaffold(
        containerColor = palette.background,
        contentColor = palette.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Mis apuntes · #$tallerId") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Apuntes privados de tu cuenta.", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = state.texto,
                onValueChange = vm::editar,
                label = { Text("Nuevo apunte") },
                enabled = !state.ocupado,
                minLines = 3,
                maxLines = 7,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { vm.guardar(tallerId) },
                enabled = !state.ocupado && state.texto.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar en este teléfono") }
            OutlinedButton(onClick = { vm.sincronizar(tallerId) }, enabled = !state.ocupado,
                modifier = Modifier.fillMaxWidth()) { Text("Sincronizar apuntes") }
            if (state.ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.mensaje.isNotBlank()) Text(state.mensaje,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (state.pendientes.isNotEmpty()) {
                Text("En el teléfono · pendientes", style = MaterialTheme.typography.titleLarge)
                state.pendientes.forEach { ApunteCard(it.comentario.orEmpty()) }
            }
            if (state.publicados.isNotEmpty()) {
                Text("Confirmados en la nube", style = MaterialTheme.typography.titleLarge)
                state.publicados.forEach { ApunteCard(it.texto) }
            }
        }
    }
}

@Composable
private fun ApunteCard(texto: String) {
    val palette = MaterialTheme.colorScheme
    OutlinedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = palette.surface, contentColor = palette.onSurface)
    ) { Text(texto, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge) }
}
