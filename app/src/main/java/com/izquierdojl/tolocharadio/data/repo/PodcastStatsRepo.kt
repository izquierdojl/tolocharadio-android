package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.PodcastCategories
import com.izquierdojl.tolocharadio.data.remote.api.PodcastHabits
import com.izquierdojl.tolocharadio.data.remote.api.PodcastRecents
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopEpisodes
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopPodcasts
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastStatsSummaryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estadísticas privadas de podcast (US6, FR-026). Lectura directa
 * del servidor para la cuenta activa.
 */
@Singleton
class PodcastStatsRepo
    @Inject
    constructor(private val api: PodcastsApi) {
        suspend fun summary(): ApiResult<PodcastStatsSummaryDto> = safeCall { api.statsSummary() }

        suspend fun topPodcasts(limit: Int? = null): ApiResult<PodcastTopPodcasts> = safeCall { api.topPodcasts(limit) }

        suspend fun topEpisodes(limit: Int? = null): ApiResult<PodcastTopEpisodes> = safeCall { api.topEpisodes(limit) }

        suspend fun timeline(granularity: String? = null): ApiResult<PodcastTimelineDto> {
            return safeCall { api.timeline(granularity) }
        }

        suspend fun habits(): ApiResult<PodcastHabits> = safeCall { api.habits() }

        suspend fun categories(limit: Int? = null): ApiResult<PodcastCategories> = safeCall { api.categories(limit) }

        suspend fun recent(limit: Int? = null): ApiResult<PodcastRecents> = safeCall { api.recent(limit) }
    }
