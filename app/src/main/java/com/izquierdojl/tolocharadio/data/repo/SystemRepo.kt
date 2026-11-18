package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.izquierdojl.tolocharadio.data.remote.dto.AppConfigDto
import com.izquierdojl.tolocharadio.data.remote.dto.podcastDownloadsEnabled
import com.izquierdojl.tolocharadio.data.remote.dto.podcastQueueMax
import com.izquierdojl.tolocharadio.data.remote.dto.podcastsEnabled
import javax.inject.Inject
import javax.inject.Singleton

/** Capacidades de podcast del servidor activo (research D1). */
data class PodcastCapabilities(
    val podcasts: Boolean = false,
    val downloads: Boolean = false,
    val queueMax: Int = 200,
)

/** Sistema: health + config pública de la instancia. */
@Singleton
class SystemRepo
    @Inject
    constructor(private val api: SystemApi) {
        /** true si la instancia responde. Nunca lanza: es una sonda. */
        suspend fun isHealthy(): Boolean = runCatching { api.health().isSuccessful }.getOrDefault(false)

        /** Config (`appName`, `registrationEnabled`). */
        suspend fun config(): ApiResult<AppConfigDto> = safeCall { api.config() }

        /** Capacidades de podcast; ausentes cuando el servidor no las publica (solo Radio). */
        suspend fun podcastCapabilities(): ApiResult<PodcastCapabilities> {
            return when (val r = safeCall { api.config() }) {
                is ApiResult.Ok ->
                    ApiResult.Ok(
                        PodcastCapabilities(
                            podcasts = r.value.podcastsEnabled(),
                            downloads = r.value.podcastDownloadsEnabled(),
                            queueMax = r.value.podcastQueueMax(),
                        ),
                    )
                is ApiResult.Err -> r
            }
        }
    }
