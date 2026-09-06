package com.izquierdojl.tolocharadio.core.session

import com.izquierdojl.tolocharadio.data.remote.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de sesión observado por la navegación (`RequireAuth`). */
sealed interface AuthState {
    data object Loading : AuthState

    data class Authenticated(val user: UserDto) : AuthState

    data class Unauthenticated(val reason: String? = null) : AuthState
}

/**
 * Sesión en memoria: access JWT + usuario. El refresh cifrado vive en
 * [TokenStore]. Toda renovación pasa por aquí (un solo vuelo).
 */
@Singleton
class SessionManager
    @Inject
    constructor(private val tokens: TokenStore) {
        private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
        val authState: StateFlow<AuthState> = _authState.asStateFlow()

        @Volatile
        private var accessToken: String? = null

        /** Access actual para el interceptor (llamado en hilo IO). */
        fun accessTokenNow(): String? = accessToken

        /** Marca sesión válida tras login/register/refresh. */
        fun setAuthenticated(
            user: UserDto,
            access: String,
            refresh: String,
        ) {
            accessToken = access
            tokens.setRefresh(refresh)
            _authState.value = AuthState.Authenticated(user)
        }

        /** Actualiza usuario (p. ej. tras `PATCH /users/me`). */
        fun setUser(user: UserDto) {
            if (_authState.value is AuthState.Authenticated) {
                _authState.value = AuthState.Authenticated(user)
            }
        }

        /** Actualiza el access tras un refresh. */
        fun setAccess(access: String) {
            accessToken = access
        }

        /** Cierra la sesión y borra secretos. */
        fun logout(reason: String? = null) {
            accessToken = null
            tokens.setRefresh(null)
            _authState.value = AuthState.Unauthenticated(reason)
        }

        /** Intento de restauración al arrancar (el refresh se valida con `/users/me`). */
        fun markRestoring() {
            _authState.value = AuthState.Loading
        }
    }

