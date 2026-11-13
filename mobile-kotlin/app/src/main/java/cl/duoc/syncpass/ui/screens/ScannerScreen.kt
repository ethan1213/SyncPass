package cl.duoc.syncpass.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.layout.AREA_TACTIL_MINIMA
import cl.duoc.syncpass.ui.syncPassViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Acreditacion por QR.
 *
 * La vista se ocupa de la camara y de los permisos, que son cosa del sistema
 * operativo; la validacion del codigo y el guardado viven en [ScannerViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(repository: SyncPassRepository, tallerId: Long, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val vm = syncPassViewModel(repository) { ScannerViewModel(it) }
    val state by vm.state.collectAsState()

    var camaraPermitida by remember { mutableStateOf(tienePermiso(context, Manifest.permission.CAMERA)) }
    val pedirCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        camaraPermitida = it
    }
    var ubicacionPermitida by remember { mutableStateOf(tieneUbicacion(context)) }
    val pedirUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        ubicacionPermitida = it.values.any { concedido -> concedido }
        if (!ubicacionPermitida) vm.onUbicacionDenegada()
    }

    val previewView = remember { PreviewView(context) }
    val lector = remember { BarcodeScanning.getClient() }

    LaunchedEffect(Unit) { if (!camaraPermitida) pedirCamara.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(tallerId) { vm.cargar(tallerId) }

    DisposableEffect(camaraPermitida) {
        if (camaraPermitida) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analisis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val imagen = imageProxy.image
                    if (imagen == null) imageProxy.close()
                    else lector.process(InputImage.fromMediaImage(imagen, imageProxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codigos ->
                            codigos.firstOrNull { it.rawValue?.matches(PATRON_PARTICIPANTE) == true }
                                ?.rawValue?.let(vm::onQrLeido)
                        }.addOnCompleteListener { imageProxy.close() }
                }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analisis)
                } catch (_: Exception) {
                    vm.onCamaraNoDisponible()
                }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            val future = ProcessCameraProvider.getInstance(context)
            if (future.isDone) future.get().unbindAll()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Acreditación QR") },
            navigationIcon = { TextButton(onClick = onNavigateBack) { Text("←") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Taller $tallerId")

            if (camaraPermitida) AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxWidth().height(260.dp)
            ) else Text("Sin permiso de cámara. Puedes escribir el código.")

            OutlinedTextField(
                value = state.code,
                onValueChange = vm::onCode,
                label = { Text("Código del participante") },
                modifier = Modifier.fillMaxWidth()
            )

            if (state.message.isNotEmpty()) Text(state.message)

            if (state.exigeGeocerca) {
                Text("Este taller exige una ubicación GPS precisa dentro de su radio.")
                if (!ubicacionPermitida) TextButton(onClick = {
                    pedirUbicacion.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Permitir ubicación") }
            }

            Button(
                onClick = { vm.guardar(tallerId, ubicacionPermitida) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
            ) {
                Text("Guardar asistencia")
            }
        }
    }
}

private fun tienePermiso(context: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

private fun tieneUbicacion(context: Context): Boolean =
    tienePermiso(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
        tienePermiso(context, Manifest.permission.ACCESS_COARSE_LOCATION)
