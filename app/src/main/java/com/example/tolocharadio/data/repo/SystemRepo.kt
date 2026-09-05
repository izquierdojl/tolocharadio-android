package com.example.tolocharadio.data.repo

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.safeCall
import com.example.tolocharadio.data.remote.api.SystemApi
import com.example.tolocharadio.data.remote.dto.AppConfigDto
import javax.inject.Inject
import javax.inject.Singleton

/** Sistema: health + config pública de la instancia. */
@Singleton
class SystemRepo
    @Inject
    constructor(private val api: SystemApi) {
        /** true si la instancia responde. Nunca lanza: es una sonda. */
        suspend fun isHealthy(): Boolean = runCatching { api.health().isSuccessful }.getOrDefault(false)

        /** Config (`appName`, `registrationEnabled`). */
        suspend fun config(): ApiResult<AppConfigDto> = safeCall { api.config() }
    }
