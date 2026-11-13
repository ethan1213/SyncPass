package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.syncpass.data.AuthSession

/** El invitado abre un único taller identificado antes de consultar el catálogo. */
@Composable
fun EntryScreen(vm: EntryViewModel, onLogin: () -> Unit, onContinue: () -> Unit,
    onGuestTaller: (Long) -> Unit, onScanQr: () -> Unit) {
    var code by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center) {
        Text("SyncPass", fontSize = 30.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("CITT Plaza Vespucio · Vinculación con el medio",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))
        Text("En terreno.\nSin papel.", fontSize = 36.sp, lineHeight = 40.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(12.dp))
        Text("Ingresa a tu cuenta para acreditar, responder encuestas y sincronizar registros.",
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(24.dp))
        Button(onClick = if (AuthSession.accessToken == null) onLogin else onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (AuthSession.accessToken == null) "Iniciar sesión o crear cuenta" else "Continuar con mi cuenta")
        }
        Spacer(Modifier.height(30.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))
        Text("Explorar como invitado", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Escribe el número del taller. Podrás consultar ese taller sin iniciar sesión.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = code,
            onValueChange = { code = it.filter(Char::isDigit).take(12); vm.clearError() },
            label = { Text("Código numérico del taller") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (vm.error.isNotBlank()) Text(vm.error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            val id = code.toLongOrNull() ?: return@OutlinedButton
            vm.openGuest(id, onGuestTaller)
        }, enabled = !vm.busy && code.toLongOrNull() != null,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (vm.busy) "Buscando taller…" else "Ver taller como invitado")
        }
        TextButton(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
            Text("Escanear QR del taller")
        }
    }
}
