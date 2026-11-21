package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.InscripcionDto
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.TallerDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MisInscripcionesUiState(
    val talleres: List<TallerDto> = emptyList(),
    val pendientes: Set<Long> = emptySet(),
    val asistidos: Set<Long> = emptySet(),
    val cargando: Boolean = false,
    val marcandoId: Long? = null,
    val error: String = ""
)

/** RF12: talleres que el participante marco "voy a asistir" y aun no acredita. */
class MisInscripcionesViewModel(private val repository: SyncPassRepository) : ViewModel() {
    private val _state = MutableStateFlow(MisInscripcionesUiState())
    val state = _state.asStateFlow()

    fun cargar(token: String?) = viewModelScope.launch {
        if (token == null) { _state.update { it.copy(error = "Necesitas iniciar sesión.") }; return@launch }
        _state.update { it.copy(cargando = true, error = "") }
        runCatching {
            val talleres = repository.cachedTalleres().ifEmpty { repository.catalog().rows }
            val mias = repository.misInscripciones(token)
            val pendientes = mias.filter { it.estado == "pendiente" }.map(InscripcionDto::tallerId).toSet()
            val asistidos = mias.filter { it.estado == "asistio" }.map(InscripcionDto::tallerId).toSet()
            Triple(talleres, pendientes, asistidos)
        }.onSuccess { (talleres, pendientes, asistidos) ->
            _state.update { it.copy(talleres = talleres, pendientes = pendientes, asistidos = asistidos, cargando = false) }
        }.onFailure { e ->
            _state.update { it.copy(cargando = false, error = e.message ?: "No se pudo cargar.") }
        }
    }

    fun marcar(tallerId: Long, token: String?) = viewModelScope.launch {
        if (token == null) return@launch
        _state.update { it.copy(marcandoId = tallerId, error = "") }
        runCatching { repository.marcarPendiente(tallerId, token) }
            .onSuccess { _state.update { it.copy(marcandoId = null, pendientes = it.pendientes + tallerId) } }
            .onFailure { e -> _state.update { it.copy(marcandoId = null, error = e.message ?: "No se pudo marcar.") } }
    }
}
