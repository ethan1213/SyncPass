package cl.duoc.syncpass.ui.screens

import android.Manifest
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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/** Lee exclusivamente códigos de taller SPT:<id>; los QR SP1: son de participantes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestQrScreen(vm: EntryViewModel, onGuestTaller: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var message by remember { mutableStateOf("Apunta la cámara al QR del taller.") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) message = "Permite la cámara o vuelve para escribir el código del taller."
    }
    val previewView = remember { PreviewView(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    val handled = remember { AtomicBoolean(false) }

    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    DisposableEffect(granted) {
        if (granted) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                    val image = proxy.image
                    if (image == null) proxy.close()
                    else scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes ->
                            val id = codes.firstNotNullOfOrNull { code ->
                                Regex("^SPT:([1-9][0-9]*)$").matchEntire(code.rawValue.orEmpty())
                                    ?.groupValues?.get(1)?.toLongOrNull()
                            }
                            if (id != null && handled.compareAndSet(false, true)) {
                                message = "Buscando taller #$id…"
                                vm.openGuest(id, onGuestTaller) {
                                    message = vm.error
                                    handled.set(false)
                                }
                            }
                        }.addOnCompleteListener { proxy.close() }
                }
                try { provider.unbindAll(); provider.bindToLifecycle(lifecycle,
                    CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }
                catch (_: Exception) { message = "No se pudo abrir la cámara. Usa el código numérico." }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            val provider = ProcessCameraProvider.getInstance(context)
            if (provider.isDone) provider.get().unbindAll()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Escanear QR del taller") },
        navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(16.dp))
            if (granted) AndroidView(factory = { previewView },
                modifier = Modifier.fillMaxWidth().height(360.dp))
            else OutlinedButton(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
                Text("Permitir cámara")
            }
            Spacer(Modifier.height(12.dp))
            Text("Este QR abre un taller. El QR de participante se usa después para acreditar asistencia.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
