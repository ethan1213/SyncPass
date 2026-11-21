package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.PendingRecord
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SurveyUiState(
    val rating: Int = 5,
    val comment: String = "",
    val message: String = "",
    val busy: Boolean = false
)

/**
 * Encuesta de satisfaccion.
 *
 * Siempre escribe primero en la cola local de Room: la respuesta queda a salvo
 * aunque no haya señal, y viaja al servidor en el siguiente envio.
 */
class SurveyViewModel(private val repository: SyncPassRepository) : ViewModel() {

    private val _state = MutableStateFlow(SurveyUiState())
    val state = _state.asStateFlow()

    fun onRating(valor: Int) = _state.update { it.copy(rating = valor.coerceIn(1, 5)) }

    fun onComment(valor: String) = _state.update { it.copy(comment = valor.take(2000)) }

    fun guardar(tallerId: Long) {
        val owner = AuthSession.participantId
        if (owner == null) {
            _state.update { it.copy(message = "Inicia sesión antes de responder.") }
            return
        }
        if (tallerId <= 0) {
            _state.update { it.copy(message = "Selecciona un taller.") }
            return
        }
        val actual = _state.value
        if (actual.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching {
                repository.queue(PendingRecord(owner = owner, tipo = "encuesta", tallerId = tallerId,
                    rating = actual.rating, comentario = actual.comment, utilidad = "muy_util"))
            }.onSuccess {
                _state.update {
                    it.copy(busy = false, comment = "",
                        message = "Encuesta guardada. Se envía sola al recuperar la conexión.")
                }
                AuthSession.refreshed()
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = mensajeDeError(error)) }
            }
        }
    }
}
