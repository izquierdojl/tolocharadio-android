package com.izquierdojl.tolocharadio.domain.auth

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.session.AuthState
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import javax.inject.Inject

/**
 * Restaura la sesión al arrancar la app. Lee el refresh token de
 * [TokenStore], lo valida con POST /auth/refresh y actualiza
 * [SessionManager].
 *
 * FR-006b: si el refresh está revocado, re-autentica silenciosamente
 * con el password cifrado del servidor activo (sin pedir nada al
 * usuario) y renueva el token.
 *
 * Caso de uso puro del domain layer (constitución I).
 */
class RestoreSessionUseCase
    @Inject
    constructor(
        private val authRepo: AuthRepo,
        private val session: SessionManager,
        private val tokens: TokenStore,
    ) {
        /**
         * Intenta restaurar la sesión. Idempotente — solo actúa si
         * el estado actual es [AuthState.Loading].
         *
         * @return [AuthState] resultante tras el intento de restauración
         */
        suspend operator fun invoke(): AuthState {
            if (session.authState.value !is AuthState.Loading) {
                return session.authState.value
            }

            val refresh = tokens.getRefresh()
            if (refresh == null) {
                // FR-006: sin refresh (servidor nuevo añadido con
                // credenciales guardadas), auto-login con el password
                // cifrado del servidor activo.
                if (!silentReLogin(null)) session.logout()
                return session.authState.value
            }

            return when (val result = authRepo.refreshNow(refresh)) {
                is ApiResult.Ok -> {
                    persistActiveRefresh()
                    session.authState.value
                }
                is ApiResult.Err -> {
                    // FR-006b: re-login silencioso con password guardado
                    val relogged = silentReLogin(result)
                    if (!relogged) session.logout()
                    session.authState.value
                }
            }
        }

        /**
         * Re-login con password cifrado; true si la sesión quedó restaurada.
         * Con [refreshError] null (sin refresh) o `Unauthorized` (revocado)
         * se reintenta; con otros errores (red, servidor caído) no.
         */
        private suspend fun silentReLogin(refreshError: ApiResult.Err?): Boolean {
            if (refreshError != null && refreshError.error !is DomainError.Unauthorized) return false
            val serverId = tokens.getActiveServerId() ?: return false
            val creds = tokens.getServerCredentials(serverId) ?: return false
            val email = creds.email ?: return false
            val password = creds.password ?: return false

            return when (authRepo.login(email, password)) {
                is ApiResult.Ok -> {
                    persistActiveRefresh()
                    true
                }
                is ApiResult.Err -> false
            }
        }

        /** Guarda el refresh renovado bajo las credenciales del servidor activo. */
        private suspend fun persistActiveRefresh() {
            val serverId = tokens.getActiveServerId() ?: return
            val creds = tokens.getServerCredentials(serverId) ?: return
            tokens.setServerCredentials(
                serverId,
                refresh = tokens.getRefresh(),
                email = creds.email,
                password = creds.password,
            )
        }
    }
