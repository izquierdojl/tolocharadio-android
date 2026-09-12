package com.izquierdojl.tolocharadio.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de la sesión en memoria (sin datos de perfil). */
sealed interface SessionState {
    /** Sin sesión (aún no autenticado o limpiada). */
    data object Idle : SessionState

    /** Login/refresh en curso. */
    data object Authenticating : SessionState

    /** Sesión lista: hay access token en memoria. */
    data object Ready : SessionState
}

/**
 * Sesión en memoria: solo el **access token** (el refresh y las
 * credenciales viven cifrados en [TokenStore]). Nunca se persiste.
 */
@Singleton
class SessionManager
    @Inject
    constructor() {
        private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
        val state: StateFlow<SessionState> = _state.asStateFlow()

        @Volatile
        private var accessToken: String? = null

        /** Access actual para el interceptor y el player (hilo IO). */
        fun accessTokenNow(): String? = accessToken

        /** Marca sesión autenticada con un access nuevo. */
        fun setAccess(access: String) {
            accessToken = access
            _state.value = SessionState.Ready
        }

        /** Marca login/refresh en curso. */
        fun setAuthenticating() {
            _state.value = SessionState.Authenticating
        }

        /** Limpia la sesión (cambio de servidor o error de credenciales). */
        fun clear() {
            accessToken = null
            _state.value = SessionState.Idle
        }
    }
