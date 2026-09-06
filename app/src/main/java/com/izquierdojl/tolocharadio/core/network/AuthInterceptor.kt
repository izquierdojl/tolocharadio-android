package com.izquierdojl.tolocharadio.core.network

import com.izquierdojl.tolocharadio.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adjunta `Authorization: Bearer <access>` a cada llamada `/api/v1`
 * cuando hay sesión. Nunca toca URLs ni logs (FR-007).
 */
class AuthInterceptor(private val session: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.accessTokenNow()
        val request =
            if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
        return chain.proceed(request)
    }
}
