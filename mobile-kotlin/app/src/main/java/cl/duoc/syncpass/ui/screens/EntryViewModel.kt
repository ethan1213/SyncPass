package cl.duoc.syncpass.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.ESTADO_PENDIENTE
import cl.duoc.syncpass.data.mensajeDeError
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.TallerDto
import kotlinx.coroutines.launch

class EntryViewModel(private val repository: SyncPassRepository) : ViewModel() {
    init {
        viewModelScope.launch {
            repository.online.collect { connected ->
                if (connected) {
                    val result = runCatching { repository.flushEntries(AuthSession.accessToken) }.getOrNull()
                    if (result != null && result.enviados > 0) {
                        attendance = "Asistencia sincronizada con el servidor."
                        selectedTaller?.id?.let { refreshTaller(it) }
                    }
                }
            }
        }
    }
    var selectedTaller by mutableStateOf<TallerDto?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf("")
        private set
    var attendance by mutableStateOf("")
        private set
    var recordingMessage by mutableStateOf("")
        private set
    var offline by mutableStateOf(false)
        private set
    val guestName get() = repository.guestName()

    fun clearError() { error = "" }

    fun openGuest(id: Long, onFound: (Long) -> Unit, onNotFound: () -> Unit = {}) {
        if (busy || id <= 0) return
        busy = true
        error = ""
        viewModelScope.launch {
            // El repositorio elige la fuente: nube si hay red, Room si no.
            val snapshot = repository.catalog()
            val talleres = snapshot.rows
            offline = snapshot.fromCache
            busy = false
            val taller = talleres.firstOrNull { it.id == id }
            if (taller != null) {
                selectedTaller = taller
                attendance = ""
                recordingMessage = ""
                onFound(id)
                viewModelScope.launch {
                    recordingMessage = runCatching { repository.recordingStatus(id).mensaje }.getOrDefault("")
                }
                if (taller.latitud == null) AuthSession.accessToken?.let { token -> joinAccount(id, token) }
            } else {
                error = if (snapshot.fromCache)
                    "Sin conexión y el taller no está guardado en el teléfono. Conéctate e intenta de nuevo."
                else "No encontramos ese taller. Revisa el número."
                onNotFound()
            }
        }
    }

    fun joinGuest(name: String) {
        val id = selectedTaller?.id ?: return
        if (busy || name.isBlank()) { error = "Escribe tu nombre para registrar la asistencia."; return }
        busy = true; error = ""
        viewModelScope.launch {
            runCatching { repository.joinGuest(id, name) }
                .onSuccess {
                    attendance = if (it.estado == ESTADO_PENDIENTE)
                        "Sin conexión: tu asistencia quedó guardada en el teléfono y se enviará al volver la señal."
                    else "Asistencia registrada como invitado."
                    refreshTaller(id)
                }
                .onFailure { error = mensajeDeError(it) }
            busy = false
        }
    }

    fun joinAccount(id: Long, token: String) {
        if (busy) return
        busy = true; error = ""
        viewModelScope.launch {
            runCatching { repository.joinAccount(id, token) }
                .onSuccess {
                    attendance = when (it.estado) {
                        "ya_presente" -> "Ya figurabas presente en el taller."
                        ESTADO_PENDIENTE -> "Sin conexión: tu asistencia quedó guardada y se enviará al volver la señal."
                        else -> "Asistencia registrada con tu cuenta."
                    }
                    refreshTaller(id)
                }
                .onFailure { error = mensajeDeError(it) }
            busy = false
        }
    }

    private suspend fun refreshTaller(id: Long) {
        selectedTaller = runCatching { repository.catalog().rows.firstOrNull { it.id == id } }
            .getOrNull() ?: selectedTaller
    }
}
