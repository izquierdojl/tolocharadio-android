package com.izquierdojl.tolocharadio.core.network

import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.api.AuthApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/** JSON tolerante del backend (ignora campos nuevos del OpenAPI). */
val TolochaJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

/**
 * Construye Retrofit contra `{baseUrl}/api/v1` con la sesión por
 * servidor: `Bearer` en cada petición y refresh/re-login automático
 * ante 401. La base cambia al cambiar de servidor.
 */
object RetrofitFactory {
    fun create(
        baseUrl: String,
        session: SessionManager,
        tokens: TokenStore,
        authApi: dagger.Lazy<AuthApi>,
        debug: Boolean,
    ): Retrofit {
        val logging =
            HttpLoggingInterceptor().apply {
                level =
                    if (debug) {
                        HttpLoggingInterceptor.Level.HEADERS
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                redactHeader("Authorization")
            }
        val client =
            OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(session))
                .authenticator(TokenAuthenticator(session, tokens, authApi))
                .addInterceptor(logging)
                .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/api/v1/")
            .client(client)
            .addConverterFactory(TolochaJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
