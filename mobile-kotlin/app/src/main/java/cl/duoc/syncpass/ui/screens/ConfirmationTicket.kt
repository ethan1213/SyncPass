package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Qué tan definitivo es el resultado que se muestra en el ticket. */
enum class TicketEstado { NINGUNO, GUARDADO_LOCAL, ENVIADO, ERROR }

/**
 * Confirmación grande y visible (a diferencia de un Text suelto) de que un
 * registro se guardó. [GUARDADO_LOCAL] avisa que quedó en el teléfono y se
 * enviará solo al recuperar señal; [ENVIADO] confirma que el servidor ya lo
 * aceptó. Pedido explícito: que no pase inadvertido como antes.
 */
@Composable
fun TicketConfirmacion(mensaje: String, estado: TicketEstado, modifier: Modifier = Modifier) {
    if (estado == TicketEstado.NINGUNO || mensaje.isBlank()) return
    val (fondo, icono) = when (estado) {
        TicketEstado.GUARDADO_LOCAL -> Color(0xFF8D6E00) to "⏳"
        TicketEstado.ENVIADO -> Color(0xFF1B5E20) to "✔"
        TicketEstado.ERROR -> Color(0xFFB3261E) to "✕"
        TicketEstado.NINGUNO -> return
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = fondo),
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icono, fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                mensaje,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
}
