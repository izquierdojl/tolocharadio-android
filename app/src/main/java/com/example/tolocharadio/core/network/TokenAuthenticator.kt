package com.example.tolocharadio.core.network

import com.example.tolocharadio.core.session.SessionManager
import com.example.tolocharadio.core.session.TokenStore
import com.example.tolocharadio.data.remote.api.AuthApi
import com.example.tolocharadio.data.remote.api.RefreshBody
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Renueva el access ante un 401 y reintenta la llamada UNA vez
 * (research R2). Si el refresh falla → logout + Login.
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
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null
        val newAccess =
            runBlocking {
                mutex.withLock {
                    // Otro hilo ya renovó mientras esperábamos.
                    val current = session.accessTokenNow()
                    val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
                    if (current != null && current != sent) return@withLock current
                    val refresh = tokens.getRefresh() ?: return@withLock null
                    val result = runCatching { authApi.get().refresh(RefreshBody(refresh)) }.getOrNull()
                    val body = if (result?.isSuccessful == true) result.body() else null
                    if (body == null) {
                        session.logout("expired")
                        null
                    } else {
                        session.setAuthenticated(body.user, body.accessToken, body.refreshToken)
                        body.accessToken
                    }
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
