package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.InstanceApiFactory
import com.izquierdojl.tolocharadio.data.remote.api.LoginBody
import com.izquierdojl.tolocharadio.data.remote.api.RefreshBody
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth por servidor: login y refresh **contra la URL de ese servidor**
 * (no contra la baseUrl activa del grafo), para que el alta, la edición
 * y el cambio de servidor se validen contra la instancia correcta.
 */
@Singleton
class AuthRepo
    @Inject
    constructor(
        private val session: SessionManager,
        private val tokens: TokenStore,
        private val apiFactory: InstanceApiFactory,
    ) {
        /** Serializa [ensureSession] para no rotar el refresh en paralelo. */
        private val mutex = Mutex()

        /** Login con las credenciales del servidor; en éxito guarda credenciales y sesión. */
        suspend fun login(
            serverId: String,
            baseUrl: String,
            email: String,
            password: String,
        ): ApiResult<Unit> {
            session.setAuthenticating()
            return when (val r = safeCall { apiFactory.authApi(baseUrl).login(LoginBody(email.trim(), password)) }) {
                is ApiResult.Ok -> {
                    session.setAccess(r.value.accessToken)
                    tokens.setCredentials(serverId, email.trim(), password, r.value.refreshToken)
                    ApiResult.Ok(Unit)
                }
                is ApiResult.Err -> {
                    session.clear()
                    r
                }
            }
        }

        /** Renueva el access con el refresh guardado del servidor. */
        suspend fun refresh(
            serverId: String,
            baseUrl: String,
        ): ApiResult<Unit> {
            val refresh =
                tokens.getCredentials(serverId)?.refresh
                    ?: return ApiResult.Err(DomainError.Unauthorized("missing_refresh"))
            return when (val r = safeCall { apiFactory.authApi(baseUrl).refresh(RefreshBody(refresh)) }) {
                is ApiResult.Ok -> {
                    session.setAccess(r.value.accessToken)
                    tokens.updateRefresh(serverId, r.value.refreshToken)
                    ApiResult.Ok(Unit)
                }
                is ApiResult.Err -> r
            }
        }

        /**
         * Asegura sesión para un servidor: intenta refresh y, si falla,
         * re-loguea con email/contraseña guardados. Sin credenciales →
         * error de credenciales (la UI abre el formulario). Serializado
         * con [mutex] para no rotar el refresh token en paralelo.
         */
        suspend fun ensureSession(
            serverId: String,
            baseUrl: String,
        ): ApiResult<Unit> =
            mutex.withLock {
                val creds = tokens.getCredentials(serverId)
                if (creds == null || creds.email.isNullOrBlank() || creds.password.isNullOrBlank()) {
                    return@withLock ApiResult.Err(DomainError.Unauthorized("missing_credentials"))
                }
                if (!creds.refresh.isNullOrBlank()) {
                    when (val r = refresh(serverId, baseUrl)) {
                        is ApiResult.Ok -> return@withLock r
                        is ApiResult.Err -> Unit // cae a login con credenciales
                    }
                }
                login(serverId, baseUrl, creds.email, creds.password)
            }
    }
