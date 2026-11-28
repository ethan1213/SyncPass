package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TallerCodigoScreen(tallerId: Long, volver: () -> Unit) {
    val qr = remember(tallerId) { makeQr("SPT:$tallerId") }
    Scaffold(topBar = {
        TopAppBar(title = { Text("QR del taller #$tallerId") }, navigationIcon = {
            TextButton(onClick = volver) { Text("Volver") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Los participantes pueden apuntar con «Escanear QR del taller» para registrar su asistencia.",
                color = MaterialTheme.colorScheme.onBackground)
            if (qr != null) Image(qr, "QR de asistencia del taller $tallerId",
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f))
            else Text("No se pudo generar el QR. Vuelve a abrir el taller.")
        }
    }
}
