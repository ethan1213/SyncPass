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

data class BitacoraUiState(
    val bitacora: BitacoraDto? = null,
    val question: String = "",
    val answer: RespuestaBitacora? = null,
    val status: String = "Cargando bitácora...",
    val busy: Boolean = false
)

/** Resumen de la sesion y preguntas respondidas con cita al texto publicado. */
class BitacoraViewModel(private val repository: SyncPassRepository) : ViewModel() {

    private val _state = MutableStateFlow(BitacoraUiState())
    val state = _state.asStateFlow()

    fun onQuestion(valor: String) = _state.update { it.copy(question = valor.take(300)) }

    fun cargar(tallerId: Long) {
        val token = AuthSession.accessToken
        if (token == null) {
            _state.update { it.copy(status = "Inicia sesión para consultar la bitácora.") }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { repository.bitacora(tallerId, token) }
                .onSuccess { datos -> _state.update { it.copy(bitacora = datos, status = "", busy = false) } }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, status = mensajeDeError(error)) }
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
