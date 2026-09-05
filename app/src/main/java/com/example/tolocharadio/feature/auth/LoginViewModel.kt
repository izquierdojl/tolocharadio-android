package com.example.tolocharadio.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.userMessage
import com.example.tolocharadio.data.repo.AuthRepo
import com.example.tolocharadio.data.repo.SystemRepo
import com.example.tolocharadio.domain.ValidateAuthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI del login. */
sealed interface LoginUiState {
    data class Form(
        val email: String = "",
        val password: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val generalError: String? = null,
        val loading: Boolean = false,
        val registrationEnabled: Boolean = true,
    ) : LoginUiState
}

/** Login con email+password; oculta registro si la instancia lo cierra (US-2/3). */
@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val auth: AuthRepo,
        private val system: SystemRepo,
        private val validate: ValidateAuthUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<LoginUiState>(LoginUiState.Form(loading = true))
        val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

        init {
            viewModelScope.launch {
                val enabled =
                    when (val r = system.config()) {
                        is ApiResult.Ok -> r.value.registrationEnabled
                        is ApiResult.Err -> true
                    }
                _ui.value = LoginUiState.Form(registrationEnabled = enabled)
            }
        }

        fun onEmailChange(v: String) = update { copy(email = v, emailError = null, generalError = null) }

        fun onPasswordChange(v: String) = update { copy(password = v, passwordError = null, generalError = null) }

        /** Valida local y llama a `POST /auth/login`. */
        fun login(onLoggedIn: () -> Unit) {
            val form = _ui.value as? LoginUiState.Form ?: return
            val emailErr = validate.email(form.email)
            val passErr = if (form.password.isEmpty()) "Introduce tu contraseña." else null
            if (emailErr != null || passErr != null) {
                _ui.value = form.copy(emailError = emailErr, passwordError = passErr)
                return
            }
            _ui.value = form.copy(loading = true, generalError = null)
            viewModelScope.launch {
                when (val r = auth.login(form.email, form.password)) {
                    is ApiResult.Ok -> onLoggedIn()
                    is ApiResult.Err -> _ui.value = form.copy(loading = false, generalError = r.error.userMessage())
                }
            }
        }

        private fun update(block: LoginUiState.Form.() -> LoginUiState.Form) {
            _ui.value = (_ui.value as? LoginUiState.Form)?.block() ?: return
        }
    }
