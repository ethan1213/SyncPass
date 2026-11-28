package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.ApunteRemoto
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.PendingRecord
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApuntesUiState(
    val texto: String = "",
    val pendientes: List<PendingRecord> = emptyList(),
    val publicados: List<ApunteRemoto> = emptyList(),
    val mensaje: String = "",
    val ocupado: Boolean = false
)

class ApuntesViewModel(private val repository: SyncPassRepository) : ViewModel() {
    private val _state = MutableStateFlow(ApuntesUiState())
    val state = _state.asStateFlow()

    fun editar(texto: String) = _state.update { it.copy(texto = texto.take(2000)) }

    fun cargar(tallerId: Long) = operar(tallerId) { owner, token ->
        leerPendientes(tallerId, owner)
        if (repository.isOnline()) leerNube(tallerId, token)
        else _state.update { it.copy(mensaje = "Puedes guardar apuntes en este teléfono sin conexión.") }
    }

    fun guardar(tallerId: Long) {
        val texto = _state.value.texto.trim()
        if (texto.isBlank()) return
        operar(tallerId) { owner, _ ->
            repository.queue(PendingRecord(owner = owner, tipo = "apunte", tallerId = tallerId, comentario = texto))
            leerPendientes(tallerId, owner)
            _state.update { it.copy(texto = "", mensaje = "Apunte guardado en el teléfono.") }
            AuthSession.refreshed()
        }
    }

    fun sincronizar(tallerId: Long) = operar(tallerId) { owner, token ->
        repository.sync(owner, token)
        leerPendientes(tallerId, owner)
        leerNube(tallerId, token)
        _state.update {
            it.copy(mensaje = if (it.pendientes.isEmpty()) "Apuntes actualizados desde la nube."
                else "${it.pendientes.size} apunte(s) continúan pendientes.")
        }
        AuthSession.refreshed()
    }

    private suspend fun leerPendientes(tallerId: Long, owner: String) {
        val notas = repository.pending(owner).filter { it.tipo == "apunte" && it.tallerId == tallerId }
        _state.update { it.copy(pendientes = notas) }
    }

    private suspend fun leerNube(tallerId: Long, token: String) {
        val notas = repository.misApuntes(tallerId, token)
        _state.update { it.copy(publicados = notas) }
    }

    private fun operar(tallerId: Long, tarea: suspend (String, String) -> Unit) {
        val owner = AuthSession.participantId
        val token = AuthSession.accessToken
        if (owner == null || token == null) {
            _state.value = ApuntesUiState(mensaje = "Inicia sesión para abrir tus apuntes.")
            return
        }
        if (tallerId <= 0 || _state.value.ocupado) return
        _state.update { it.copy(ocupado = true, mensaje = "") }
        viewModelScope.launch {
            runCatching { tarea(owner, token) }
                .onFailure { error -> _state.update { it.copy(mensaje = mensajeDeError(error)) } }
            _state.update { it.copy(ocupado = false) }
        }
    }
}
