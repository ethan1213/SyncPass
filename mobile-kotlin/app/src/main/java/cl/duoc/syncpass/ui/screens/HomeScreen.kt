package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.syncpass.data.model.TallerComunitario
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.layout.AREA_TACTIL_MINIMA
import cl.duoc.syncpass.ui.syncPassViewModel
import cl.duoc.syncpass.ui.layout.LocalFormatoPantalla

/**
 * Panel de talleres en terreno.
 *
 * Es adaptativa: una columna en telefono (operable con una mano) y dos o tres
 * columnas en tablet, segun la clase de ancho de ventana (caso §3.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: SyncPassRepository,
    guestTallerId: Long?,
    onNavigateToScanner: (Long) -> Unit,
    onNavigateToSurvey: (Long) -> Unit,
    onNavigateToBitacora: (Long) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val formato = LocalFormatoPantalla.current
    val vm = syncPassViewModel(repository) { HomeViewModel(it) }
    val state by vm.state.collectAsState()
    val rows = if (guestTallerId == null) state.talleres else state.talleres.filter { it.id == guestTallerId }
    val pending = state.pending
    val message = state.message
    val owner = AuthSession.participantId
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(owner, AuthSession.syncVersion) { vm.refreshPending(owner) }
    DisposableEffect(lifecycleOwner, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshCatalog()
                if (owner != null) vm.refreshPending(owner)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val talleres = rows.map { TallerComunitario(it.id.toInt(), it.title, it.area.orEmpty(),
        it.unidadVecinal.orEmpty(), it.comuna.orEmpty(), it.fechaHora.orEmpty(),
        it.cuposMaximos, it.inscritos) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SyncPass",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "CITT Plaza Vespucio · En terreno",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Estado de conexion siempre visible: en terreno el monitor
                    // necesita saber si esta acumulando registros locales.
                    Surface(
                        color = if (state.offline) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = when {
                                state.offline && state.waiting -> "Esperando señal · ${state.pending + state.entries}"
                                state.offline -> "Catálogo local"
                                else -> "API conectada"
                            },
                            fontSize = 11.sp,
                            color = if (state.offline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.sizeIn(
                            minWidth = AREA_TACTIL_MINIMA,
                            minHeight = AREA_TACTIL_MINIMA
                        )
                    ) {
                        Text("Cuenta", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(formato.columnas),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = formato.margenLateral),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // La tarjeta de accion y el titulo ocupan siempre el ancho completo.
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("CITT PLAZA VESPUCIO · VINCULACIÓN CON EL MEDIO",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text("En terreno.\nSin papel.", fontSize = 32.sp,
                            fontWeight = FontWeight.Bold, lineHeight = 35.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Text("Talleres, acreditación y encuestas en un solo flujo. Los registros pendientes se envían al recuperar la conexión.",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    if (owner == null) Button(onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)) {
                        Text("Iniciar sesión o registrarse")
                    }
                    else {
                        Text("Mi panel · ${AuthSession.roles.joinToString().ifBlank { "sin asignar" }}",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        if (message.isNotEmpty()) Text(message)
                        val qr = remember(owner) { makeQr("SP1:$owner") }
                        qr?.let { Image(it, contentDescription = "Mi QR de acreditación", modifier = Modifier.size(220.dp)) }
                        Text(if (state.waiting)
                            "${pending + state.entries} registro(s) en espera: se envían solos al volver la conexión."
                        else "$pending registros pendientes de sincronización")
                        Button(onClick = {
                            val token = AuthSession.accessToken
                            if (token != null) {
                                vm.sync(owner, token)
                            }
                        }, enabled = pending > 0) { Text("Sincronizar ahora") }
                    }
                    if (AuthSession.roles.any { it in listOf("admin","coordinador","monitor") })
                        TarjetaAccesoEscaner { talleres.firstOrNull()?.let { onNavigateToScanner(it.id.toLong()) } }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (guestTallerId == null) "Talleres en terreno" else "Taller #$guestTallerId",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(talleres) { taller ->
                TarjetaTaller(
                    taller = taller,
                    canScan = AuthSession.roles.any { it in listOf("admin","coordinador","monitor") },
                    onAcreditar = { onNavigateToScanner(taller.id.toLong()) },
                    onEncuestar = { onNavigateToSurvey(taller.id.toLong()) },
                    onBitacora = { onNavigateToBitacora(taller.id.toLong()) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun makeQr(value: String): ImageBitmap? = runCatching {
    val bits = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 320, 320)
    val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
    for (y in 0 until 320) for (x in 0 until 320)
        bitmap.setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    bitmap.asImageBitmap()
}.getOrNull()

@Composable
private fun TarjetaAccesoEscaner(onNavigateToScanner: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Acreditación en segundos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Lee el QR de cada participante. Si falta señal, el registro queda guardado en el teléfono.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Button(
                onClick = onNavigateToScanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AREA_TACTIL_MINIMA)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Abrir escáner QR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TarjetaTaller(
    taller: TallerComunitario,
    canScan: Boolean,
    onAcreditar: () -> Unit,
    onEncuestar: () -> Unit,
    onBitacora: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = taller.areaTematica.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "#${taller.id} · ${taller.inscritos}/${taller.cuposMaximos} cupos",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = taller.titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${taller.unidadVecinal} · ${taller.comuna}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Barra de quorum: indicador visual inmediato para el coordinador.
            LinearProgressIndicator(
                progress = {
                    (taller.inscritos.toFloat() / taller.cuposMaximos.coerceAtLeast(1).toFloat())
                        .coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canScan) Button(
                    onClick = onAcreditar,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = AREA_TACTIL_MINIMA)
                ) {
                    Text("Acreditar QR", fontSize = 12.sp)
                }
                if (AuthSession.accessToken != null) OutlinedButton(
                    onClick = onEncuestar,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = AREA_TACTIL_MINIMA)
                ) {
                Text("Encuesta", fontSize = 12.sp)
                }
            }
            if (AuthSession.accessToken != null) TextButton(onClick = onBitacora) {
                Text("Resumen y preguntas del taller")
            }
        }
    }
}
