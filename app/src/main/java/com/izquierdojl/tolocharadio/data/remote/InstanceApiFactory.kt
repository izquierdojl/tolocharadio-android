package com.izquierdojl.tolocharadio.data.remote

import com.izquierdojl.tolocharadio.core.network.TolochaJson
import com.izquierdojl.tolocharadio.data.remote.api.AuthApi
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Construye clientes contra una instancia **arbitraria** (sin sesión):
 * se usa para validar/autenticar un servidor antes de que su URL pase
 * a ser la del grafo Hilt. Evita el bug de autenticar contra la
 * `baseUrl` compilada o contra el servidor activo anterior.
 */
interface InstanceApiFactory {
    fun authApi(baseUrl: String): AuthApi

    fun systemApi(baseUrl: String): SystemApi
}

@Singleton
class DefaultInstanceApiFactory
    @Inject
    constructor() : InstanceApiFactory {
        private val client: OkHttpClient by lazy {
            OkHttpClient
                .Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        private fun retrofit(baseUrl: String): Retrofit =
            Retrofit
                .Builder()
                .baseUrl(baseUrl.trimEnd('/') + "/api/v1/")
                .client(client)
                .addConverterFactory(TolochaJson.asConverterFactory("application/json".toMediaType()))
                .build()

        override fun authApi(baseUrl: String): AuthApi = retrofit(baseUrl).create(AuthApi::class.java)

        override fun systemApi(baseUrl: String): SystemApi = retrofit(baseUrl).create(SystemApi::class.java)
    }
