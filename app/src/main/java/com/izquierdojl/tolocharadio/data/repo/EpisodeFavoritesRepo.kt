package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.EpisodeIdBody
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Favoritos de episodios (US5, FR-023). Independientes de las
 * emisoras favoritas; la verdad es el servidor, sin caché local.
 */
@Singleton
class EpisodeFavoritesRepo
    @Inject
    constructor(private val api: PodcastsApi) {
        private val _favorites = MutableStateFlow<List<PodcastEpisode>>(emptyList())
        val favorites: StateFlow<List<PodcastEpisode>> = _favorites.asStateFlow()

        private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
        val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

        private val _loaded = MutableStateFlow(false)

        /** `true` cuando el servidor ya confirmó la lista o una mutación. */
        val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

        suspend fun list(): ApiResult<List<PodcastEpisode>> {
            return when (val r = safeCall { api.favorites() }) {
                is ApiResult.Ok -> {
                    publish(r.value.items)
                    ApiResult.Ok(r.value.items)
                }
                is ApiResult.Err -> r
            }
        }

        suspend fun add(episodeId: Long): ApiResult<PodcastEpisode> {
            return when (val r = safeCall { api.addFavorite(EpisodeIdBody(episodeId)) }) {
                is ApiResult.Ok -> {
                    _favoriteIds.value = _favoriteIds.value + episodeId
                    _favorites.value = _favorites.value.filterNot { it.id == episodeId } + r.value.episode
                    _loaded.value = true
                    ApiResult.Ok(r.value.episode)
                }
                is ApiResult.Err -> r
            }
        }

        /**
         * Quita un favorito. Idempotente: 404 se reconcilia como éxito.
         */
        suspend fun remove(episodeId: Long): ApiResult<Unit> {
            return when (val r = safeCall { api.removeFavorite(episodeId) }) {
                is ApiResult.Ok -> {
                    drop(episodeId)
                    ApiResult.Ok(Unit)
                }
                is ApiResult.Err -> {
                    if (r.error is DomainError.NotFound) {
                        drop(episodeId)
                        ApiResult.Ok(Unit)
                    } else {
                        r
                    }
                }
            }
        }

        /** Alterna el favorito según su estado conocido. */
        suspend fun toggle(
            episodeId: Long,
            isFavorite: Boolean,
        ): ApiResult<Unit> {
            return when {
                isFavorite -> remove(episodeId)
                else ->
                    when (val r = add(episodeId)) {
                        is ApiResult.Ok -> ApiResult.Ok(Unit)
                        is ApiResult.Err -> r
                    }
            }
        }

        private fun publish(items: List<PodcastEpisode>) {
            _favorites.value = items
            _favoriteIds.value = items.map { it.id }.toSet()
            _loaded.value = true
        }

        private fun drop(episodeId: Long) {
            _favoriteIds.value = _favoriteIds.value - episodeId
            _favorites.value = _favorites.value.filterNot { it.id == episodeId }
            _loaded.value = true
        }
    }
