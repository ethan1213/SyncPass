package cl.duoc.syncpass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val nickname: String = "",
    val password: String = "",
    val code: String = "",
    /** true cuando Cognito ya envio el codigo y falta confirmarlo. */
    val esperandoCodigo: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
) {
    val puedeRegistrar: Boolean
        get() = !busy && username.isNotBlank() && email.isNotBlank() && nickname.isNotBlank() && password.isNotBlank()
    val puedeConfirmar: Boolean get() = !busy && code.isNotBlank()
}

/** Registro en dos pasos: crear la cuenta y confirmar el codigo del correo. */
class RegisterViewModel(private val repository: SyncPassRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state = _state.asStateFlow()

    fun onUsername(valor: String) = _state.update { it.copy(username = valor, error = null) }
    fun onEmail(valor: String) = _state.update { it.copy(email = valor, error = null) }
    fun onNickname(valor: String) = _state.update { it.copy(nickname = valor, error = null) }
    fun onPassword(valor: String) = _state.update { it.copy(password = valor, error = null) }
    fun onCode(valor: String) = _state.update { it.copy(code = valor, error = null) }

    fun registrar(onCuentaLista: () -> Unit) {
        val actual = _state.value
        if (!actual.puedeRegistrar) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repository.register(actual.username.trim(), actual.email.trim(),
                    actual.nickname.trim(), actual.password)
            }.onSuccess { resultado ->
                // La contraseña no se conserva en memoria mas alla del envio.
                _state.update {
                    it.copy(busy = false, password = "", esperandoCodigo = !resultado.userConfirmed,
                        message = if (resultado.userConfirmed) null
                        else "Enviamos un código a ${resultado.destination}.")
                }
                if (resultado.userConfirmed) onCuentaLista()
            }.onFailure { error ->
                _state.update { it.copy(busy = false, error = mensajeDeError(error)) }
            }
        }
    }

    fun confirmar(onConfirmado: () -> Unit) {
        val actual = _state.value
        if (!actual.puedeConfirmar) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.confirm(actual.username.trim(), actual.code.trim()) }
                .onSuccess {
                    _state.update { it.copy(busy = false) }
                    onConfirmado()
                }
                .onFailure { error -> _state.update { it.copy(busy = false, error = mensajeDeError(error)) } }
        }
    }

    fun reenviarCodigo() {
        val actual = _state.value
        if (actual.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.resend(actual.username.trim()) }
                .onSuccess { _state.update { it.copy(busy = false, message = "Enviamos un nuevo código.") } }
                .onFailure { error -> _state.update { it.copy(busy = false, error = mensajeDeError(error)) } }
        }
    }
}
