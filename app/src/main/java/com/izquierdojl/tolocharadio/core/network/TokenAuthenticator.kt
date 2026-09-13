package com.izquierdojl.tolocharadio.core.network

import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.api.AuthApi
import com.izquierdojl.tolocharadio.data.remote.api.LoginBody
import com.izquierdojl.tolocharadio.data.remote.api.RefreshBody
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Códigos HTTP que rechazan credenciales de forma definitiva. */
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403

/**
 * Ante un 401: renovación **single-flight** con el refresh guardado y
 * reintento único; si el refresh falla, re-login con email/contraseña
 * cifrados del servidor activo (FR-005).
 *
 * Solo un rechazo definitivo (401/403) de refresh y login limpia la
 * sesión y deja que la UI ofrezca editar el servidor. Los fallos
 * transitorios (IOException, 5xx) conservan la sesión para reintentar
 * cuando vuelva la red, sin obligar al usuario a re-autenticarse.
 */
class TokenAuthenticator(
    private val session: SessionManager,
    private val tokens: TokenStore,
    private val authApi: dagger.Lazy<AuthApi>,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (responseCount(response) >= 2) return null
        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh")) return null

        val newAccess =
            runBlocking {
                mutex.withLock {
                    // Otro hilo ya renovó mientras esperábamos.
                    val current = session.accessTokenNow()
                    val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
                    if (current != null && current != sent) return@withLock current

                    val serverId = tokens.getActiveServerId() ?: return@withLock null
                    val creds = tokens.getCredentials(serverId)

                    // 1) Refresh.
                    var refreshRejected = false
                    if (!creds?.refresh.isNullOrBlank()) {
                        val result = runCatching { authApi.get().refresh(RefreshBody(creds.refresh)) }.getOrNull()
                        val body = result?.takeIf { it.isSuccessful }?.body()
                        if (body != null) {
                            session.setAccess(body.accessToken)
                            tokens.updateRefresh(serverId, body.refreshToken)
                            return@withLock body.accessToken
                        }
                        refreshRejected = isDefinitiveRejection(result?.code())
                    }

                    // 2) Re-login con credenciales guardadas.
                    val email = creds?.email
                    val password = creds?.password
                    var loginRejected = false
                    var loginAttempted = false
                    if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                        loginAttempted = true
                        val result = runCatching { authApi.get().login(LoginBody(email, password)) }.getOrNull()
                        val body = result?.takeIf { it.isSuccessful }?.body()
                        if (body != null) {
                            session.setAccess(body.accessToken)
                            tokens.setCredentials(serverId, email, password, body.refreshToken)
                            return@withLock body.accessToken
                        }
                        loginRejected = isDefinitiveRejection(result?.code())
                    }

                    // Fallo transitorio con login pendiente: no se invalida la sesión.
                    if (loginRejected || (!loginAttempted && refreshRejected)) {
                        session.clear()
                    }
                    null
                }
            } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    /** true solo si la respuesta rechaza credenciales (401/403); null es transitorio. */
    private fun isDefinitiveRejection(httpCode: Int?): Boolean {
        return httpCode == HTTP_UNAUTHORIZED || httpCode == HTTP_FORBIDDEN
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
