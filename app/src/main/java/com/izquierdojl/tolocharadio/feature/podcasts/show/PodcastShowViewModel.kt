package com.izquierdojl.tolocharadio.feature.podcasts.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.EpisodesRepo
import com.izquierdojl.tolocharadio.domain.podcasts.SubscribePodcastUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.UnsubscribePodcastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Filtros de episodio (paridad web: todos, sin empezar, en progreso, completados). */
enum class EpisodeFilter(val apiValue: String) {
    ALL("all"),
    UNPLAYED("unplayed"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
}

/** Órdenes de episodio (paridad web). */
enum class EpisodeSort(val apiValue: String) {
    RECENT("recent"),
    TITLE("title"),
    DURATION("duration"),
}

/** Página de la ficha (paridad web: 20 por página). */
const val SHOW_PAGE_SIZE = 20

/** UI de la ficha del programa (US6). */
data class PodcastShowUi(
    val podcast: PodcastSummary? = null,
    val items: List<PodcastEpisode> = emptyList(),
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    val sort: EpisodeSort = EpisodeSort.RECENT,
    val query: String = "",
    val message: String? = null,
)

/** Ficha del programa con episodios filtrables, ordenables y paginados (US6, FR-011/FR-012). */
@HiltViewModel
class PodcastShowViewModel
    @Inject
    constructor(
        private val repo: EpisodesRepo,
        private val subscribe: SubscribePodcastUseCase,
        private val unsubscribe: UnsubscribePodcastUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(PodcastShowUi())
        val ui: StateFlow<PodcastShowUi> = _ui.asStateFlow()

        private var podcastId: Long = 0
        private var offset: Int = 0

        fun open(podcastId: Long) {
            if (this.podcastId == podcastId && _ui.value.podcast != null) return
            this.podcastId = podcastId
            reload()
        }

        fun setFilter(filter: EpisodeFilter) {
            _ui.value = _ui.value.copy(filter = filter)
            reload()
        }

        fun setSort(sort: EpisodeSort) {
            _ui.value = _ui.value.copy(sort = sort)
            reload()
        }

        fun setQuery(query: String) {
            _ui.value = _ui.value.copy(query = query)
        }

        fun search() {
            reload()
        }

        /** Recarga la ficha manteniendo filtro, orden y búsqueda (tras cambiar estado). */
        fun refresh() {
            reload(clearItems = false)
        }

        fun loadMore() {
            val current = _ui.value
            if (current.loadingMore || !current.hasMore) return
            viewModelScope.launch {
                _ui.value = current.copy(loadingMore = true)
                when (
                    val r =
                        repo.episodes(
                            podcastId,
                            sort = current.sort.apiValue,
                            filter = current.filter.apiValue,
                            query = current.query.ifBlank { null },
                            offset = offset,
                            limit = SHOW_PAGE_SIZE,
                        )
                ) {
                    is ApiResult.Ok -> {
                        offset += r.value.items.size
                        _ui.value =
                            current.copy(
                                items = (current.items + r.value.items).distinctBy { it.id },
                                hasMore = r.value.pagination.hasMore,
                                loadingMore = false,
                            )
                    }
                    is ApiResult.Err ->
                        _ui.value = current.copy(loadingMore = false, message = r.error.userMessage())
                }
            }
        }

        fun clearMessage() {
            _ui.value = _ui.value.copy(message = null)
        }

        /** Suscribe o da de baja desde la ficha (FR-008). */
        fun toggleSubscribe() {
            val podcast = _ui.value.podcast ?: return
            viewModelScope.launch {
                val result =
                    if (podcast.isSubscribed) {
                        unsubscribe(podcast.id)
                    } else {
                        subscribe(podcast.id)
                    }
                when (result) {
                    is ApiResult.Ok -> reload()
                    is ApiResult.Err -> _ui.value = _ui.value.copy(message = result.error.userMessage())
                }
            }
        }

        private fun reload(clearItems: Boolean = true) {
            val current = _ui.value
            offset = 0
            viewModelScope.launch {
                _ui.value =
                    current.copy(
                        loading = true,
                        message = null,
                        items = if (clearItems) emptyList() else current.items,
                    )
                when (val d = repo.detail(podcastId)) {
                    is ApiResult.Ok -> {
                        when (
                            val r =
                                repo.episodes(
                                    podcastId,
                                    sort = _ui.value.sort.apiValue,
                                    filter = _ui.value.filter.apiValue,
                                    query = _ui.value.query.ifBlank { null },
                                    offset = 0,
                                    limit = SHOW_PAGE_SIZE,
                                )
                        ) {
                            is ApiResult.Ok -> {
                                offset = r.value.items.size
                                _ui.value =
                                    _ui.value.copy(
                                        podcast = d.value,
                                        items = r.value.items,
                                        hasMore = r.value.pagination.hasMore,
                                        loading = false,
                                    )
                            }
                            is ApiResult.Err ->
                                _ui.value =
                                    _ui.value.copy(podcast = d.value, loading = false, message = r.error.userMessage())
                        }
                    }
                    is ApiResult.Err -> _ui.value = _ui.value.copy(loading = false, message = d.error.userMessage())
                }
            }
        }
    }
