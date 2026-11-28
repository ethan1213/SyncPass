package cl.duoc.syncpass.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

private val tallerQr = Regex("SPT:([1-9][0-9]*)", RegexOption.IGNORE_CASE)
internal fun tallerIdDelQr(texto: String?): Long? =
    texto?.trim()?.let { tallerQr.matchEntire(it)?.groupValues?.get(1)?.toLongOrNull() }

/** Lee exclusivamente códigos de taller SPT:<id>; los QR SP1: son de participantes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestQrScreen(vm: EntryViewModel, onGuestTaller: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var message by remember { mutableStateOf("Apunta la cámara al QR del taller.") }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) message = "Permite la cámara o elige la imagen del QR."
    }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    val handled = remember { AtomicBoolean(false) }
    val active = remember { AtomicBoolean(true) }

    fun leer(codes: List<Barcode>, desdeArchivo: Boolean = false) {
        if (!active.get() || handled.get()) return
        val id = codes.firstNotNullOfOrNull { tallerIdDelQr(it.rawValue) }
        if (id == null) {
            if (codes.isNotEmpty() || desdeArchivo)
                message = "No se encontró un QR de taller. Abre «Mostrar QR» en la web."
            return
        }
        if (handled.compareAndSet(false, true)) {
            message = "QR leído. Buscando taller #$id…"
            vm.openGuest(id, onGuestTaller) {
                message = vm.error
                handled.set(false)
            }
        }
    }
    val elegirQr = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { InputImage.fromFilePath(context, uri) }
                .onSuccess { imagen ->
                    scanner.process(imagen).addOnSuccessListener { leer(it, desdeArchivo = true) }
                        .addOnFailureListener { message = "No se pudo leer la imagen del QR." }
                }.onFailure { message = "No se pudo abrir la imagen del QR." }
        }
    }

    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    DisposableEffect(scanner) {
        onDispose {
            active.set(false)
            scanner.close()
        }
    }
    DisposableEffect(granted) {
        var conectado = true
        var preview: Preview? = null
        var analysis: ImageAnalysis? = null
        val future = ProcessCameraProvider.getInstance(context)
        if (granted) {
            future.addListener({
                if (!conectado || !active.get()) return@addListener
                val livePreview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val liveAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                preview = livePreview
                analysis = liveAnalysis
                liveAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                    val image = proxy.image
                    if (image == null || !active.get() || handled.get()) proxy.close()
                    else scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { leer(it) }
                        .addOnFailureListener { message = "La cámara no pudo leer el QR. Puedes elegir la imagen." }
                        .addOnCompleteListener { proxy.close() }
                }
                try {
                    camera = future.get().bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, livePreview, liveAnalysis)
                } catch (_: Exception) {
                    message = "No se pudo abrir la cámara. Puedes elegir la imagen del QR."
                }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            conectado = false
            analysis?.clearAnalyzer()
            if (future.isDone && preview != null && analysis != null)
                runCatching { future.get().unbind(preview!!, analysis!!) }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Escanear QR del taller") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(16.dp))
            if (granted) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().height(300.dp))
                OutlinedButton(onClick = {
                    val punto = previewView.meteringPointFactory.createPoint(previewView.width / 2f, previewView.height / 2f)
                    camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(punto).build())
                }, enabled = camera != null) { Text("Enfocar QR") }
            }
            else OutlinedButton(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
                Text("Permitir cámara")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { elegirQr.launch("image/*") }) { Text("Leer imagen del QR") }
            Text("Usa el QR del taller que aparece en «Mostrar QR» en la web.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
