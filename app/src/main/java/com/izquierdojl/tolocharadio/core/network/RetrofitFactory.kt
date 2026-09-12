package com.izquierdojl.tolocharadio.core.network

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
 * Construye Retrofit contra `{baseUrl}/api/v1`. La app no usa
 * autenticación de usuario: no se añade ninguna credencial. La base
 * cambia al cambiar de servidor: el cliente se recrea (ver `NetworkModule`).
 */
object RetrofitFactory {
    fun create(
        baseUrl: String,
        debug: Boolean,
    ): Retrofit {
        val client = createHttpClient(debug)
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/api/v1/")
            .client(client)
            .addConverterFactory(TolochaJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    /** Cliente OkHttp sin autenticación de usuario: no añade `Authorization` (FR-011). */
    internal fun createHttpClient(debug: Boolean): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                level =
                    if (debug) {
                        HttpLoggingInterceptor.Level.HEADERS
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
            }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }
}
