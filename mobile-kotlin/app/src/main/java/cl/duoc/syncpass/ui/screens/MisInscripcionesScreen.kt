package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.syncPassViewModel

/**
 * RF12: lista los talleres y deja marcar "voy a asistir" (preinscripcion en el
 * servidor). La acreditacion real por QR es la que confirma la asistencia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisInscripcionesScreen(repository: SyncPassRepository, onVolver: () -> Unit) {
    val vm = syncPassViewModel(repository) { MisInscripcionesViewModel(it) }
    val state by vm.state.collectAsState()
    val token = AuthSession.accessToken
    LaunchedEffect(token) { vm.cargar(token) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Mis talleres pendientes") }, navigationIcon = {
            TextButton(onClick = onVolver) { Text("Volver") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Marca los talleres a los que piensas asistir.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
            if (state.cargando) CircularProgressIndicator()
            LazyColumn {
                items(state.talleres) { taller ->
                    val pendiente = state.pendientes.contains(taller.id)
                    val asistido = state.asistidos.contains(taller.id)
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(taller.title, style = MaterialTheme.typography.titleMedium)
                            Text(taller.comuna.orEmpty(), style = MaterialTheme.typography.bodySmall)
                            if (!taller.rolesPermitidos.isNullOrBlank() || !taller.zonaRestringida.isNullOrBlank()) {
                                Text("Restringido: ${taller.rolesPermitidos ?: ""} ${taller.zonaRestringida ?: ""}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            when {
                                asistido -> Text("✔ Asististe", color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium)
                                pendiente -> Text("Pendiente de asistir", color = MaterialTheme.colorScheme.primary)
                                else -> Button(onClick = { vm.marcar(taller.id, token) },
                                    enabled = state.marcandoId != taller.id) {
                                    Text(if (state.marcandoId == taller.id) "Marcando…" else "Marcar que voy")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
