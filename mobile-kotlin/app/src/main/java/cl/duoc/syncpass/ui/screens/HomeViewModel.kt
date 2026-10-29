package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.TallerDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val talleres: List<TallerDto> = emptyList(),
    val pending: Int = 0,
    val message: String = "",
    val offline: Boolean = false
)

class HomeViewModel(private val repository: SyncPassRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.cachedTalleres() }.onSuccess { rows ->
                _state.update { it.copy(talleres = rows) }
            }
            refreshCatalog()
        }
    }

    fun refreshCatalog() = viewModelScope.launch {
        try {
            val rows = repository.talleres()
            _state.update { it.copy(talleres = rows, message = "", offline = false) }
        } catch (_: Exception) {
            _state.update { it.copy(message = "Sin conexión. Se muestra el catálogo guardado.", offline = true) }
        }
    }

    fun refreshPending(owner: String?) = viewModelScope.launch {
        val count = if (owner == null) 0 else repository.pending(owner).size
        _state.update { it.copy(pending = count) }
    }

    fun sync(owner: String, token: String) = viewModelScope.launch {
        try {
            val count = repository.sync(owner, token)
            val rows = runCatching { repository.talleres() }.getOrNull()
            _state.update { it.copy(talleres = rows ?: it.talleres,
                pending = repository.pending(owner).size,
                message = "$count registro(s) sincronizado(s).", offline = rows == null) }
        } catch (e: Exception) {
            _state.update { it.copy(message = e.message ?: "No se pudo sincronizar.") }
        }
    }
}
