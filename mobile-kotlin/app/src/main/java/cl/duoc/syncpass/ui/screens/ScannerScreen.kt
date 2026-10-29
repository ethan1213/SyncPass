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
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.PendingRecord
import cl.duoc.syncpass.data.SyncPassRepository
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(repository: SyncPassRepository, tallerId: Long, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val previewView = remember { PreviewView(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }

    DisposableEffect(granted) {
        if (granted) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val image = imageProxy.image
                    if (image == null) imageProxy.close()
                    else scanner.process(InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes ->
                            codes.firstOrNull { it.rawValue?.matches(participantPattern) == true }
                                ?.rawValue?.let { code = it; message = "QR leído. Confirma la marcación." }
                        }.addOnCompleteListener { imageProxy.close() }
                }
                try { provider.unbindAll(); provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }
                catch (_: Exception) { message = "Cámara no disponible. Ingresa el código manualmente." }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose { if (futureIsAvailable(context)) ProcessCameraProvider.getInstance(context).get().unbindAll() }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Acreditación QR") }, navigationIcon = {
        TextButton(onClick = onNavigateBack) { Text("←") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Taller $tallerId")
            if (granted) AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().height(260.dp))
            else Text("Sin permiso de cámara. Puedes escribir el código.")
            OutlinedTextField(value = code, onValueChange = { code = it.trim() },
                label = { Text("Código del participante") }, modifier = Modifier.fillMaxWidth())
            if (message.isNotEmpty()) Text(message)
            Button(onClick = {
                val owner = AuthSession.participantId
                if (owner == null) message = "Inicia sesión antes de acreditar."
                else if (AuthSession.roles.none { it in listOf("admin","coordinador","monitor") }) message = "Tu perfil no puede acreditar."
                else if (tallerId <= 0 || !code.matches(participantPattern)) message = "Selecciona un taller y lee un QR válido."
                else scope.launch {
                    try {
                        repository.queue(PendingRecord(owner = owner, tipo = "asistencia", tallerId = tallerId, participante = code))
                        message = "Asistencia guardada en el dispositivo. Sincronízala desde Inicio."
                        code = ""
                    } catch (e: Exception) { message = e.message ?: "No se pudo guardar." }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar asistencia") }
        }
    }
}

private fun futureIsAvailable(context: android.content.Context): Boolean =
    ProcessCameraProvider.getInstance(context).isDone

private val participantPattern = Regex("SP1:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
