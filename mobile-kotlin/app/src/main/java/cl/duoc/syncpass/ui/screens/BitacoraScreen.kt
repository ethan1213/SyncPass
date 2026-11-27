package cl.duoc.syncpass.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.ui.layout.AREA_TACTIL_MINIMA
import cl.duoc.syncpass.ui.syncPassViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BitacoraScreen(repository: SyncPassRepository, tallerId: Long, onBack: () -> Unit) {
    val vm = syncPassViewModel(repository) { BitacoraViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var mostrarFuentes by remember(tallerId) { mutableStateOf(false) }
    val respuestaVisible = remember { BringIntoViewRequester() }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    val lectura = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
    val preguntar = {
        keyboard?.hide()
        focus.clearFocus()
        vm.preguntar(tallerId)
    }

    LaunchedEffect(tallerId) { vm.cargar(tallerId) }

    Scaffold(
        containerColor = colors.background,
        contentColor = colors.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Bitácora · Taller #$tallerId") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val editor = state.editor
            if (editor != null) {
                EditorResumen(editor, state, vm, tallerId)
            } else {
                if (state.status.isNotBlank()) AvisoBitacora(state.status)
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.bitacora?.let { bitacora ->
                    PanelBitacora("Resumen de la sesión") {
                        SelectionContainer {
                            Text(bitacora.resumen.ifBlank { bitacora.contenido },
                                style = lectura, color = colors.onSurface)
                        }
                        if (bitacora.fragmentos.isNotEmpty()) {
                            OutlinedButton(onClick = { mostrarFuentes = !mostrarFuentes }) {
                                Text(if (mostrarFuentes) "Ocultar fuentes" else "Ver apuntes publicados y fuentes")
                            }
                        }
                        if (mostrarFuentes) {
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    bitacora.fragmentos.forEachIndexed { indice, fuente ->
                                        Text("${indice + 1}. $fuente", style = lectura, color = colors.onSurface)
                                    }
                                }
                            }
                        }
                    }
                    PanelBitacora("Preguntas del taller") {
                        Text("Consulta los apuntes publicados de esta sesión.", color = colors.onSurfaceVariant)
                        OutlinedTextField(
                            value = state.question,
                            onValueChange = vm::onQuestion,
                            label = { Text("Tu pregunta") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { if (!state.busy) preguntar() }),
                            enabled = !state.busy,
                            maxLines = 4,
                            textStyle = lectura,
                            colors = coloresCampo(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = preguntar,
                            enabled = state.question.isNotBlank() && !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
                        ) { Text("Buscar respuesta") }
                    }

                    state.answer?.let { respuesta ->
                        LaunchedEffect(respuesta) { respuestaVisible.bringIntoView() }
                        PanelBitacora("Respuesta",
                            Modifier.bringIntoViewRequester(respuestaVisible)
                                .semantics { liveRegion = LiveRegionMode.Polite }) {
                            SelectionContainer {
                                Text(respuesta.respuesta, style = lectura, color = colors.onSurface)
                            }
                            if (respuesta.fragmento > 0) {
                                Surface(
                                    color = colors.primaryContainer,
                                    contentColor = colors.onPrimaryContainer,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Fuente: fragmento ${respuesta.fragmento}", style = MaterialTheme.typography.titleSmall)
                                        val cita = respuesta.cita
                                        if (!cita.isNullOrBlank() && cita != respuesta.respuesta) {
                                            SelectionContainer { Text("“$cita”", style = lectura) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (puedeGestionarBitacora(AuthSession.roles)) {
                    Button(onClick = vm::abrirEditor, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Gestionar bitácora")
                    }
                }
                OutlinedButton(
                    onClick = { vm.cargar(tallerId) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Actualizar bitácora") }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditorResumen(editor: EditorBitacora, state: BitacoraUiState, vm: BitacoraViewModel, tallerId: Long) {
    val colors = MaterialTheme.colorScheme
    val lectura = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
    val resumenVisible = remember { BringIntoViewRequester() }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(editor.origen) {
        if (editor.origen != null) resumenVisible.bringIntoView()
    }

    PanelBitacora("Apuntes del taller") {
        Text("Escribe o pega la transcripción que servirá de fuente.", color = colors.onSurfaceVariant)
        OutlinedTextField(
            value = editor.apuntes,
            onValueChange = vm::editarApuntes,
            label = { Text("Apuntes o transcripción") },
            enabled = !state.busy,
            minLines = 4,
            maxLines = 8,
            textStyle = lectura,
            colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(editor.autorizaProveedor, vm::autorizarProveedor, enabled = !state.busy)
            Text("Permitir el envío de estos apuntes a Gemini", color = colors.onSurface)
        }
        Button(
            onClick = {
                keyboard?.hide()
                focus.clearFocus()
                vm.generarResumen(tallerId)
            },
            enabled = !state.busy && editor.apuntes.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
        ) { Text("Generar borrador") }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.status.isNotBlank()) AvisoBitacora(state.status)
    }

    PanelBitacora("Revisar y publicar", Modifier.bringIntoViewRequester(resumenVisible)) {
        editor.origen?.let {
            Text(if (it == "gemini") "Borrador preparado con Gemini." else "Borrador extractivo a partir de los apuntes.",
                color = colors.onSurfaceVariant)
        }
        OutlinedTextField(
            value = editor.resumen,
            onValueChange = vm::editarResumen,
            label = { Text("Resumen revisado") },
            enabled = !state.busy,
            minLines = 3,
            maxLines = 8,
            textStyle = lectura,
            colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { vm.publicar(tallerId) },
            enabled = !state.busy && editor.apuntes.isNotBlank() && editor.resumen.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = AREA_TACTIL_MINIMA)
        ) { Text("Publicar resumen") }
        OutlinedButton(vm::cerrarEditor, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar edición")
        }
    }
}

@Composable
private fun PanelBitacora(titulo: String, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface, contentColor = colors.onSurface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            contenido()
        }
    }
}

@Composable
private fun AvisoBitacora(mensaje: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    ) { Text(mensaje, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge) }
}

@Composable
private fun coloresCampo(): TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.background,
        unfocusedContainerColor = colors.background,
        disabledContainerColor = colors.background,
        focusedTextColor = colors.onBackground,
        unfocusedTextColor = colors.onBackground,
        disabledTextColor = colors.onBackground,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onBackground,
        unfocusedBorderColor = colors.onSurfaceVariant
    )
}
