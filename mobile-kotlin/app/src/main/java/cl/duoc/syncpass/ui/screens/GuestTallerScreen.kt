package cl.duoc.syncpass.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.syncpass.data.TallerDto
import cl.duoc.syncpass.data.AuthSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestTallerScreen(vm: EntryViewModel, onBack: () -> Unit, onLogin: () -> Unit) {
    val taller = vm.selectedTaller
    var name by remember { mutableStateOf(vm.guestName) }
    val context = LocalContext.current
    var locationGranted by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationGranted = it.values.any { allowed -> allowed }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Taller #${taller?.id ?: ""}") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (taller == null) {
                Text("No encontramos el taller. Vuelve a escanear el QR.",
                    color = MaterialTheme.colorScheme.onBackground)
                return@Column
            }
            Text("${taller.area.orEmpty().uppercase()} · ${taller.comuna.orEmpty().uppercase()}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge)
            Text(taller.title, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            if (!taller.descripcion.isNullOrBlank())
                Text(taller.descripcion, color = MaterialTheme.colorScheme.onBackground)

            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow("Fecha y hora", taller.fechaHora)
                    DetailRow("Unidad vecinal", taller.unidadVecinal)
                    DetailRow("Dirección", listOfNotNull(taller.direccion, taller.comuna)
                        .filter { it.isNotBlank() }.joinToString(", "))
                    DetailRow("Coordinador", taller.docenteLider)
                    DetailRow("Cupos", "${taller.inscritos} / ${taller.cuposMaximos}")
                }
            }
            if (vm.attendance.isNotBlank()) Text(vm.attendance, color = MaterialTheme.colorScheme.primary)
            if (vm.recordingMessage.isNotBlank()) Text(vm.recordingMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (vm.error.isNotBlank()) Text(vm.error, color = MaterialTheme.colorScheme.error)
            if (taller.latitud != null && !locationGranted) {
                Text("Este taller exige GPS para confirmar presencia.")
                TextButton(onClick = { locationPermission.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) {
                    Text("Permitir ubicación")
                }
            }
            if (AuthSession.accessToken == null) {
                Text("Puedes participar como invitado. Tu asistencia queda registrada con el nombre que indiques.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(name, { name = it }, label = { Text("Tu nombre") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { vm.joinGuest(name) }, enabled = !vm.busy && name.isNotBlank() && vm.attendance.isBlank() && (taller.latitud == null || locationGranted),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Registrar asistencia como invitado") }
                TextButton(onClick = onLogin) { Text("Ya tengo cuenta · Iniciar sesión") }
            } else if (vm.attendance.isBlank()) {
                Button(onClick = { AuthSession.accessToken?.let { vm.joinAccount(taller.id, it) } },
                    enabled = !vm.busy && (taller.latitud == null || locationGranted), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (vm.busy) "Registrando…" else "Registrar mi asistencia")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary)
    Text(value, color = MaterialTheme.colorScheme.onSurface)
}
