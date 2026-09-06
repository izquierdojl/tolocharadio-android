package com.izquierdojl.tolocharadio.feature.onboarding

import com.izquierdojl.tolocharadio.core.network.TolochaJson
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/** Valida una base candidata con `/health`+`/config` sin tocar el grafo. */
@Singleton
class InstanceValidator
    @Inject
    constructor() {
        /** true si responde como instancia TolochaRadio válida. Nunca lanza: es una sonda. */
        suspend fun validate(baseUrl: String): Boolean =
            runCatching {
                val api =
                    Retrofit.Builder()
                        .baseUrl(baseUrl.trimEnd('/') + "/api/v1/")
                        .client(OkHttpClient())
                        .addConverterFactory(TolochaJson.asConverterFactory("application/json".toMediaType()))
                        .build()
                        .create(SystemApi::class.java)
                api.health().isSuccessful && api.config().isSuccessful
            }.getOrDefault(false)
    }
