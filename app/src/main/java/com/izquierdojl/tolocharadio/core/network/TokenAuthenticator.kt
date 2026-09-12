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

/**
 * Ante un 401: renovación **single-flight** con el refresh guardado y
 * reintento único; si el refresh falla, re-login con email/contraseña
 * cifrados del servidor activo (FR-005). Si nada funciona, limpia la
 * sesión y deja que la UI ofrezca editar el servidor.
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
                    if (!creds?.refresh.isNullOrBlank()) {
                        val result = runCatching { authApi.get().refresh(RefreshBody(creds?.refresh)) }.getOrNull()
                        val body = if (result?.isSuccessful == true) result.body() else null
                        if (body != null) {
                            session.setAccess(body.accessToken)
                            tokens.updateRefresh(serverId, body.refreshToken)
                            return@withLock body.accessToken
                        }
                    }

                    // 2) Re-login con credenciales guardadas.
                    val email = creds?.email
                    val password = creds?.password
                    if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                        val result = runCatching { authApi.get().login(LoginBody(email, password)) }.getOrNull()
                        val body = if (result?.isSuccessful == true) result.body() else null
                        if (body != null) {
                            session.setAccess(body.accessToken)
                            tokens.setCredentials(serverId, email, password, body.refreshToken)
                            return@withLock body.accessToken
                        }
                    }

                    session.clear()
                    null
                }
            } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
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
