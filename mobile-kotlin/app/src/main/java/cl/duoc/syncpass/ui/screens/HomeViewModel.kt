package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.TallerDto
import cl.duoc.syncpass.data.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class HomeUiState(
    val talleres: List<TallerDto> = emptyList(),
    val pending: Int = 0,
    val message: String = "",
    val offline: Boolean = false,
    val waiting: Boolean = false,
    val entries: Int = 0,
    val ticket: TicketEstado = TicketEstado.NINGUNO,
    /** RF12: talleres marcados "voy a asistir" y ya "asistio", para el badge en cada tarjeta. */
    val inscripcionesPendientes: Set<Long> = emptySet(),
    val inscripcionesAsistidas: Set<Long> = emptySet()
)

class HomeViewModel(private val repository: SyncPassRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()
    private var actualizacionCatalogo: Job? = null

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
                        "Guardado en el teléfono. Se enviará automáticamente al recuperar la conexión (${enEspera + it.pending} registro(s))."
                    else "Sin conexión. Trabajas con los datos guardados en el teléfono.",
                    ticket = if (enEspera + it.pending > 0) TicketEstado.GUARDADO_LOCAL else TicketEstado.NINGUNO) }
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
            // No se toca "ticket"/"message": si acaba de mostrarse una confirmacion,
            // un ciclo posterior sin nada que enviar no debe taparla al instante.
            _state.update { it.copy(pending = pendientes.size, entries = 0, waiting = false) }
            return
        }
        _state.update { it.copy(waiting = true,
            message = "Conexión recuperada. Enviando ${enCola + pendientes.size} registro(s)...") }
        val avisos = mutableListOf<String>()
        var falloEnvio = false

        val cola = runCatching { repository.flushEntries(token) }.getOrNull()
        if (cola != null && cola.enviados > 0) avisos += "${cola.enviados} asistencia(s) enviada(s)"
        if (cola != null && cola.descartados > 0)
            avisos += "${cola.descartados} rechazada(s) por el servidor (${cola.motivo ?: "sin detalle"})"

        if (owner != null && token != null && pendientes.isNotEmpty()) {
            runCatching { repository.sync(owner, token) }
                .onSuccess {
                    if (it > 0) {
                        avisos += "$it registro(s) sincronizado(s)"
                        AuthSession.refreshed()
                    }
                    if (it < pendientes.size)
                        avisos += "Conservamos ${pendientes.size - it} registro(s) pendientes. Revisa el taller y vuelve a intentar."
                }
                .onFailure {
                    falloEnvio = true
                    avisos += mensajeDeError(it)
                }
        }

        val quedanEntries = runCatching { repository.pendingEntries() }.getOrDefault(0)
        val quedanPending = if (owner == null) 0
            else runCatching { repository.pending(owner) }.getOrNull()?.size ?: 0
        val huboEnvioExitoso = (cola?.enviados ?: 0) > 0 || avisos.any { it.contains("sincronizado") }
        _state.update { it.copy(pending = quedanPending, entries = quedanEntries,
            waiting = quedanEntries + quedanPending > 0,
            message = avisos.joinToString(" · ").ifBlank { "Conexión recuperada." },
            ticket = when {
                falloEnvio -> TicketEstado.ERROR
                quedanEntries + quedanPending > 0 -> TicketEstado.GUARDADO_LOCAL
                huboEnvioExitoso -> TicketEstado.ENVIADO
                avisos.isNotEmpty() -> TicketEstado.ERROR
                else -> it.ticket
            }) }
    }

    /** El usuario cierra la confirmación a mano: ya no desaparece sola. */
    fun descartarTicket() = _state.update { it.copy(ticket = TicketEstado.NINGUNO) }

    /** RF12: para el badge "pendiente"/"asistido" en cada tarjeta del catálogo. */
    fun refreshInscripciones(token: String?) = viewModelScope.launch {
        if (token == null) return@launch
        runCatching { repository.misInscripciones(token) }.onSuccess { mias ->
            _state.update { it.copy(
                inscripcionesPendientes = mias.filter { i -> i.estado == "pendiente" }.map { i -> i.tallerId }.toSet(),
                inscripcionesAsistidas = mias.filter { i -> i.estado == "asistio" }.map { i -> i.tallerId }.toSet()) }
        }
    }

    fun refreshCatalog(): Job {
        actualizacionCatalogo?.takeIf { it.isActive }?.let { return it }
        return viewModelScope.launch {
            val snapshot = repository.catalog()
            _state.update { it.copy(
                talleres = if (snapshot.rows.isEmpty() && snapshot.fromCache) it.talleres else snapshot.rows,
                offline = snapshot.fromCache) }
        }.also { actualizacionCatalogo = it }
    }

    fun refreshPending(owner: String?) = viewModelScope.launch {
        val count = if (owner == null) 0 else repository.pending(owner).size
        val entries = runCatching { repository.pendingEntries() }.getOrDefault(0)
        _state.update { it.copy(pending = count, entries = entries,
            waiting = count + entries > 0 && !repository.isOnline()) }
    }

    fun sync() = viewModelScope.launch {
        if (!repository.isOnline()) {
            _state.update { it.copy(waiting = true,
                message = "Sin conexión. Los registros siguen guardados en el teléfono.") }
            return@launch
        }
        sincronizarAlReconectar()
    }
}
