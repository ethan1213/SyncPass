package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.BitacoraDto
import cl.duoc.syncpass.data.RespuestaBitacora
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal fun puedeGestionarBitacora(roles: List<String>) = roles.any { it == "admin" || it == "coordinador" }

data class EditorBitacora(
    val apuntes: String,
    val resumen: String,
    val origen: String? = null,
    val autorizaProveedor: Boolean = false
)

data class BitacoraUiState(
    val bitacora: BitacoraDto? = null,
    val question: String = "",
    val answer: RespuestaBitacora? = null,
    val status: String = "Cargando bitácora...",
    val busy: Boolean = false,
    val editor: EditorBitacora? = null
)

/** Resumen de la sesion y preguntas respondidas con cita al texto publicado. */
class BitacoraViewModel(private val repository: SyncPassRepository) : ViewModel() {

    private val _state = MutableStateFlow(BitacoraUiState())
    val state = _state.asStateFlow()

    fun onQuestion(valor: String) = _state.update { it.copy(question = valor.take(300)) }

    fun abrirEditor() {
        if (!puedeGestionarBitacora(AuthSession.roles) || _state.value.busy) return
        _state.update { estado ->
            estado.copy(editor = EditorBitacora(estado.bitacora?.contenido.orEmpty(),
                estado.bitacora?.resumen.orEmpty()), status = "")
        }
    }

    fun cerrarEditor() = _state.update { if (it.busy) it else it.copy(editor = null, status = "") }

    fun editarApuntes(valor: String) = editar { copy(apuntes = valor.take(20_000), resumen = "", origen = null) }
    fun editarResumen(valor: String) = editar { copy(resumen = valor.take(3_000)) }
    fun autorizarProveedor(valor: Boolean) = editar { copy(autorizaProveedor = valor) }

    private fun editar(cambio: EditorBitacora.() -> EditorBitacora) {
        _state.update { if (it.busy) it else it.copy(editor = it.editor?.cambio()) }
    }

    fun generarResumen(tallerId: Long) {
        val token = AuthSession.accessToken ?: return
        val editor = _state.value.editor ?: return
        if (!puedeGestionarBitacora(AuthSession.roles) || _state.value.busy || editor.apuntes.isBlank()) return
        _state.update { it.copy(busy = true, status = "Preparando el resumen…") }
        viewModelScope.launch {
            runCatching { repository.prepararResumen(tallerId, token, editor.apuntes.trim(), editor.autorizaProveedor) }
                .onSuccess { borrador ->
                    _state.update { it.copy(busy = false, status = "",
                        editor = editor.copy(resumen = borrador.resumen.take(3_000), origen = borrador.origen)) }
                }
                .onFailure { error -> _state.update { it.copy(busy = false, status = mensajeDeError(error)) } }
        }
    }

    fun publicar(tallerId: Long) {
        val token = AuthSession.accessToken ?: return
        val editor = _state.value.editor ?: return
        if (!puedeGestionarBitacora(AuthSession.roles) || _state.value.busy ||
            editor.apuntes.isBlank() || editor.resumen.isBlank()) return
        _state.update { it.copy(busy = true, status = "Publicando en SyncPass…") }
        viewModelScope.launch {
            runCatching { repository.publicarBitacora(tallerId, token, editor.apuntes.trim(), editor.resumen.trim()) }
                .onSuccess { bitacora -> _state.update { it.copy(
                    busy = false, bitacora = bitacora, editor = null, answer = null,
                    status = "Resumen publicado. Ya se puede consultar en web y Android.") } }
                .onFailure { error -> _state.update { it.copy(busy = false, status = mensajeDeError(error)) } }
        }
    }

    fun cargar(tallerId: Long) {
        if (_state.value.busy) return
        val token = AuthSession.accessToken
        if (token == null) {
            _state.update { it.copy(status = "Inicia sesión para consultar la bitácora.") }
            return
        }
        _state.update { it.copy(busy = true, answer = null, status = "Cargando bitácora...") }
        viewModelScope.launch {
            runCatching { repository.bitacora(tallerId, token) }
                .onSuccess { datos ->
                    // El servidor puede responder 200 con la bitácora aún sin contenido real:
                    // mismo mensaje que un 404 en vez de dejar la pantalla en blanco.
                    val vacia = datos.resumen.isBlank() && datos.fragmentos.isEmpty()
                    _state.update { it.copy(
                        bitacora = if (vacia) null else datos,
                        status = if (vacia) "Lo aprendido aparecerá aquí cuando el equipo publique la bitácora." else "",
                        busy = false) }
                }
                .onFailure { error ->
                    val estado = if (error is HttpException && error.code() == 404)
                        "Lo aprendido aparecerá aquí cuando el equipo publique la bitácora."
                    else mensajeDeError(error)
                    _state.update { it.copy(bitacora = null, busy = false, status = estado) }
                }
        }
    }

    fun preguntar(tallerId: Long) {
        val token = AuthSession.accessToken ?: return
        val actual = _state.value
        if (actual.busy || actual.question.isBlank()) return
        _state.update { it.copy(busy = true, status = "", answer = null) }
        viewModelScope.launch {
            runCatching { repository.preguntarBitacora(tallerId, token, actual.question.trim()) }
                .onSuccess { respuesta -> _state.update { it.copy(busy = false, answer = respuesta) } }
                .onFailure { error -> _state.update { it.copy(busy = false, status = mensajeDeError(error)) } }
        }
    }
}
