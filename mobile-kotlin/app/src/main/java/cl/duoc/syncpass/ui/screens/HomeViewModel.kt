package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.AuthSession
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
    val offline: Boolean = false,
    val waiting: Boolean = false,
    val entries: Int = 0
)

class HomeViewModel(private val repository: SyncPassRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init {
        // Primero lo que ya esta en el telefono: la pantalla nunca arranca vacia.
        viewModelScope.launch {
            runCatching { repository.cachedTalleres() }.onSuccess { rows ->
                _state.update { it.copy(talleres = rows) }
            }
            refreshCatalog()
        }
        observarConexion()
    }

    /**
     * Mientras no hay red la app se queda con Room y queda a la espera; cuando
     * la conexion vuelve refresca el catalogo y envia sola la cola pendiente.
     */
    private fun observarConexion() = viewModelScope.launch {
        repository.online.collect { conectado ->
            if (!conectado) {
                val enEspera = runCatching { repository.pendingEntries() }.getOrDefault(0)
                _state.update { it.copy(offline = true, entries = enEspera,
                    waiting = enEspera + it.pending > 0,
                    message = if (enEspera + it.pending > 0)
                        "Sin conexión. ${enEspera + it.pending} registro(s) esperan el enlace para enviarse."
                    else "Sin conexión. Trabajas con los datos guardados en el teléfono.") }
                return@collect
            }
            refreshCatalog().join()
            sincronizarAlReconectar()
        }
    }

    /**
     * Vacia las dos colas locales en orden: primero las asistencias tomadas sin
     * senal (los invitados no necesitan token), despues el lote del monitor.
     */
    private suspend fun sincronizarAlReconectar() {
        val token = AuthSession.accessToken
        val owner = AuthSession.participantId
        val enCola = runCatching { repository.pendingEntries() }.getOrDefault(0)
        val pendientes = if (owner == null) emptyList()
            else runCatching { repository.pending(owner) }.getOrNull().orEmpty()
        if (enCola == 0 && pendientes.isEmpty()) {
            _state.update { it.copy(pending = pendientes.size, entries = 0, waiting = false) }
            return
        }
        _state.update { it.copy(waiting = true,
            message = "Conexión recuperada. Enviando ${enCola + pendientes.size} registro(s)...") }
        val avisos = mutableListOf<String>()

        val cola = runCatching { repository.flushEntries(token) }.getOrNull()
        if (cola != null && cola.enviados > 0) avisos += "${cola.enviados} asistencia(s) enviada(s)"
        if (cola != null && cola.descartados > 0)
            avisos += "${cola.descartados} rechazada(s) por el servidor (${cola.motivo ?: "sin detalle"})"

        if (owner != null && token != null && pendientes.isNotEmpty()) {
            runCatching { repository.sync(owner, token) }
                .onSuccess { avisos += "$it registro(s) sincronizado(s)"; AuthSession.refreshed() }
                .onFailure { avisos += (it.message ?: "no se pudo sincronizar el lote") }
        }

        val quedanEntries = runCatching { repository.pendingEntries() }.getOrDefault(0)
        val quedanPending = if (owner == null) 0
            else runCatching { repository.pending(owner) }.getOrNull()?.size ?: 0
        _state.update { it.copy(pending = quedanPending, entries = quedanEntries,
            waiting = quedanEntries + quedanPending > 0,
            message = avisos.joinToString(" · ").ifBlank { "Conexión recuperada." }) }
    }

    fun refreshCatalog() = viewModelScope.launch {
        val snapshot = repository.catalog()
        _state.update { it.copy(
            talleres = if (snapshot.rows.isEmpty() && snapshot.fromCache) it.talleres else snapshot.rows,
            offline = snapshot.fromCache,
            message = if (snapshot.fromCache) "Sin conexión. Se muestra el catálogo guardado." else "") }
    }

    fun refreshPending(owner: String?) = viewModelScope.launch {
        val count = if (owner == null) 0 else repository.pending(owner).size
        val entries = runCatching { repository.pendingEntries() }.getOrDefault(0)
        _state.update { it.copy(pending = count, entries = entries,
            waiting = count + entries > 0 && !repository.isOnline()) }
    }

    fun sync(owner: String, token: String) = viewModelScope.launch {
        try {
            val count = repository.sync(owner, token)
            val snapshot = repository.catalog()
            val quedan = repository.pending(owner).size
            _state.update { it.copy(talleres = if (snapshot.rows.isEmpty()) it.talleres else snapshot.rows,
                pending = quedan, waiting = quedan > 0,
                message = "$count registro(s) sincronizado(s).", offline = snapshot.fromCache) }
        } catch (e: Exception) {
            _state.update { it.copy(waiting = true,
                message = e.message ?: "No se pudo sincronizar. Se reintentará al volver la conexión.") }
        }
    }
}
