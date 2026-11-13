package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formato del QR que identifica a un participante (el del taller es SPT:). */
val PATRON_PARTICIPANTE =
    Regex("SP1:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

data class ScannerUiState(
    val code: String = "",
    val message: String = "",
    val exigeGeocerca: Boolean = false,
    val busy: Boolean = false
) {
    val codigoValido: Boolean get() = code.matches(PATRON_PARTICIPANTE)
}

/**
 * Acreditacion por QR a cargo del equipo.
 *
 * La asistencia se guarda siempre en la cola local y se envia despues: en
 * terreno no se puede depender de que haya señal en el momento del escaneo.
 */
class ScannerViewModel(private val repository: SyncPassRepository) : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    val state = _state.asStateFlow()

    private val rolesQueAcreditan = listOf("admin", "coordinador", "monitor")

    fun cargar(tallerId: Long) = viewModelScope.launch {
        val exige = runCatching { repository.exigeGeocerca(tallerId) }.getOrDefault(false)
        _state.update { it.copy(exigeGeocerca = exige) }
    }

    fun onCode(valor: String) = _state.update { it.copy(code = valor.trim()) }

    /** El QR leido por la camara entra por aqui, igual que el tecleado a mano. */
    fun onQrLeido(valor: String) = _state.update {
        it.copy(code = valor, message = "QR leído. Confirma la marcación.")
    }

    fun onCamaraNoDisponible() = _state.update {
        it.copy(message = "Cámara no disponible. Ingresa el código manualmente.")
    }

    fun onUbicacionDenegada() = _state.update {
        it.copy(message = "Permite la ubicación para acreditar este taller.")
    }

    fun guardar(tallerId: Long, permisoUbicacion: Boolean) {
        val actual = _state.value
        val owner = AuthSession.participantId
        val motivo = when {
            owner == null -> "Inicia sesión antes de acreditar."
            AuthSession.roles.none { it in rolesQueAcreditan } -> "Tu perfil no puede acreditar."
            tallerId <= 0 || !actual.codigoValido -> "Selecciona un taller y lee un QR válido."
            actual.exigeGeocerca && !permisoUbicacion -> "Permite la ubicación GPS para este taller."
            else -> null
        }
        if (motivo != null) {
            _state.update { it.copy(message = motivo) }
            return
        }
        if (actual.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { repository.acreditar(owner!!, tallerId, actual.code) }
                .onSuccess {
                    _state.update {
                        it.copy(busy = false, code = "",
                            message = "Asistencia guardada en el dispositivo. Se envía al recuperar la conexión.")
                    }
                    AuthSession.refreshed()
                }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, message = mensajeDeError(error)) }
                }
        }
    }
}
