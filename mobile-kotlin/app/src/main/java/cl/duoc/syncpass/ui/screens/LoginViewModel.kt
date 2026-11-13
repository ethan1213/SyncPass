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

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null
) {
    val puedeEnviar: Boolean get() = !busy && username.isNotBlank() && password.isNotBlank()
}

/** Inicio de sesion contra Cognito a traves de la API. */
class LoginViewModel(private val repository: SyncPassRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun onUsername(valor: String) = _state.update { it.copy(username = valor, error = null) }

    fun onPassword(valor: String) = _state.update { it.copy(password = valor, error = null) }

    fun iniciarSesion(onLogin: () -> Unit) {
        val actual = _state.value
        if (!actual.puedeEnviar) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.login(actual.username.trim(), actual.password) }
                .onSuccess {
                    AuthSession.signedIn(it.accessToken, it.idToken)
                    _state.update { estado -> estado.copy(busy = false, password = "") }
                    onLogin()
                }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, error = mensajeDeError(error)) }
                }
        }
    }
}
