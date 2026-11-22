package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.izquierdojl.tolocharadio.data.remote.dto.AppConfigDto
import com.izquierdojl.tolocharadio.data.remote.dto.podcastDownloadsEnabled
import com.izquierdojl.tolocharadio.data.remote.dto.podcastQueueMax
import com.izquierdojl.tolocharadio.data.remote.dto.podcastTopChartsEnabled
import com.izquierdojl.tolocharadio.data.remote.dto.podcastsEnabled
import javax.inject.Inject
import javax.inject.Singleton

/** Capacidades de podcast del servidor activo (research D1). */
data class PodcastCapabilities(
    val podcasts: Boolean = false,
    val downloads: Boolean = false,
    val queueMax: Int = 200,
    val topCharts: Boolean = false,
)

/** Sistema: health + config pública de la instancia. */
@Singleton
class SystemRepo
    @Inject
    constructor(private val api: SystemApi) {
        @Volatile
        private var cachedCapabilities: PodcastCapabilities? = null

        /** true si la instancia responde. Nunca lanza: es una sonda. */
        suspend fun isHealthy(): Boolean = runCatching { api.health().isSuccessful }.getOrDefault(false)

        /** Config (`appName`, `registrationEnabled`). */
        suspend fun config(): ApiResult<AppConfigDto> = safeCall { api.config() }

        /**
         * Capacidades de podcast; ausentes cuando el servidor no las publica
         * (solo Radio). La última respuesta correcta se cachea durante la
         * sesión para no repetir `/config` al abrir Descubrir. Un error no se
         * cachea y `forceRefresh` se usa en reintentos/refresco de modo.
         */
        suspend fun podcastCapabilities(forceRefresh: Boolean = false): ApiResult<PodcastCapabilities> {
            if (!forceRefresh) {
                cachedCapabilities?.let { return ApiResult.Ok(it) }
            }
            return when (val r = safeCall { api.config() }) {
                is ApiResult.Ok -> {
                    val capabilities =
                        PodcastCapabilities(
                            podcasts = r.value.podcastsEnabled(),
                            downloads = r.value.podcastDownloadsEnabled(),
                            queueMax = r.value.podcastQueueMax(),
                            topCharts = r.value.podcastTopChartsEnabled(),
                        )
                    cachedCapabilities = capabilities
                    ApiResult.Ok(capabilities)
                }
                is ApiResult.Err -> r
            }
        }
    }
