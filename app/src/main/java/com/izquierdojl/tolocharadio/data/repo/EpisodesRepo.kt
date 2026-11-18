package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodePage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeSearchPage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastHome
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Portada, ficha y episodios (US6, T055). Lectura del servidor con
 * paginación `hasMore`; filtros y órdenes viajan como query params.
 */
@Singleton
class EpisodesRepo
    @Inject
    constructor(private val api: PodcastsApi) {
        suspend fun home(): ApiResult<PodcastHome> = safeCall { api.home() }

        suspend fun detail(podcastId: Long): ApiResult<PodcastSummary> {
            return when (val r = safeCall { api.detail(podcastId) }) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.podcast)
                is ApiResult.Err -> r
            }
        }

        suspend fun episodes(
            podcastId: Long,
            sort: String? = null,
            filter: String? = null,
            query: String? = null,
            offset: Int = 0,
            limit: Int = 24,
        ): ApiResult<PodcastEpisodePage> = safeCall { api.episodes(podcastId, sort, filter, query, offset, limit) }

        suspend fun searchEpisodes(
            query: String,
            offset: Int = 0,
            limit: Int = 20,
        ): ApiResult<PodcastEpisodeSearchPage> = safeCall { api.searchEpisodes(query, offset, limit) }
    }
