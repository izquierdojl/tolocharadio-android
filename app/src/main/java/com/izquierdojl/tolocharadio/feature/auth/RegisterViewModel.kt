package com.izquierdojl.tolocharadio.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.domain.ValidateAuthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI del registro. */
data class RegisterForm(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val loading: Boolean = false,
)

/** Registro (solo accesible si `registrationEnabled`, US-3). */
@HiltViewModel
class RegisterViewModel
    @Inject
    constructor(
        private val auth: AuthRepo,
        private val validate: ValidateAuthUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(RegisterForm())
        val ui: StateFlow<RegisterForm> = _ui.asStateFlow()

        fun onNameChange(v: String) {
            _ui.value = _ui.value.copy(name = v, nameError = null)
        }

        fun onEmailChange(v: String) {
            _ui.value = _ui.value.copy(email = v, emailError = null)
        }

        fun onPasswordChange(v: String) {
            _ui.value = _ui.value.copy(password = v, passwordError = null)
        }

        /** Valida local y llama a `POST /auth/register`. */
        fun register(onRegistered: () -> Unit) {
            val f = _ui.value
            val nameErr = validate.name(f.name)
            val emailErr = validate.email(f.email)
            val passErr = validate.password(f.password)
            if (nameErr != null || emailErr != null || passErr != null) {
                _ui.value = f.copy(nameError = nameErr, emailError = emailErr, passwordError = passErr)
                return
            }
            _ui.value = f.copy(loading = true, generalError = null)
            viewModelScope.launch {
                when (val r = auth.register(f.email, f.password, f.name.ifBlank { null })) {
                    is ApiResult.Ok -> onRegistered()
                    is ApiResult.Err -> _ui.value = f.copy(loading = false, generalError = r.error.userMessage())
                }
            }
        }
    }

