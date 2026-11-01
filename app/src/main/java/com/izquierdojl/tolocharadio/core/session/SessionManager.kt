package com.izquierdojl.tolocharadio.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Base64
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

        @Volatile
        private var accessExpiresAtEpochSeconds: Long? = null

        /** Access actual para el interceptor y el player (hilo IO). */
        fun accessTokenNow(): String? = accessToken

        /**
         * `true` si el access JWT caduca en [skewSeconds] o menos.
         *
         * Un token opaco o sin `exp` devuelve `false`: en ese caso la renovación
         * se hará al recibir un 401.
         */
        fun isAccessTokenExpiring(
            nowEpochSeconds: Long = System.currentTimeMillis() / MILLIS_PER_SECOND,
            skewSeconds: Long = 60,
        ): Boolean {
            val expiresAt = accessExpiresAtEpochSeconds ?: return false
            return expiresAt - nowEpochSeconds <= skewSeconds
        }

        /** Marca sesión autenticada con un access nuevo. */
        fun setAccess(access: String) {
            accessToken = access
            accessExpiresAtEpochSeconds = parseJwtExpiration(access)
            _state.value = SessionState.Ready
        }

        /** Marca login/refresh en curso. */
        fun setAuthenticating() {
            _state.value = SessionState.Authenticating
        }

        /** Limpia la sesión (cambio de servidor o error de credenciales). */
        fun clear() {
            accessToken = null
            accessExpiresAtEpochSeconds = null
            _state.value = SessionState.Idle
        }

        private companion object {
            const val MILLIS_PER_SECOND = 1_000L
        }

        private fun parseJwtExpiration(token: String): Long? =
            runCatching {
                val payload = token.split('.').getOrNull(1) ?: return@runCatching(null)
                val json = String(Base64.getUrlDecoder().decode(payload))
                Json.parseToJsonElement(json).jsonObject["exp"]?.jsonPrimitive?.longOrNull
            }.getOrNull()
    }
