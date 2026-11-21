package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import com.izquierdojl.tolocharadio.domain.podcasts.SEARCH_PAGE_SIZE
import com.izquierdojl.tolocharadio.domain.podcasts.SearchSubscriptionEpisodesUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.UnsubscribePodcastUseCase
import com.izquierdojl.tolocharadio.feature.podcasts.subscriptions.SubscriptionEpisodeResult
import com.izquierdojl.tolocharadio.feature.podcasts.subscriptions.toSubscriptionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de Suscripciones (US2, FR-009). */
sealed interface SubscriptionsUiState {
    data object Loading : SubscriptionsUiState

    data object Empty : SubscriptionsUiState

    data class Content(val items: List<PodcastSummary>) : SubscriptionsUiState

    data class Error(val message: String) : SubscriptionsUiState
}

/** Ámbitos del buscador de Suscripciones (US8). */
enum class SubscriptionSearchScope {
    PODCASTS,
    EPISODES,
}

/** Estado de presentación de la búsqueda de Suscripciones (US8). */
@Suppress("LongParameterList")
data class SubscriptionSearchUi(
    val term: String = "",
    val submittedTerm: String = "",
    val scope: SubscriptionSearchScope = SubscriptionSearchScope.PODCASTS,
    val results: List<SubscriptionEpisodeResult> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val showResults: Boolean = false,
)

/**
 * Suscripciones propias con baja (US2) y búsqueda por título dentro de las
 * suscripciones (US8). El filtro de podcasts es local; la búsqueda de
 * episodios delega en el servidor y descarta respuestas obsoletas.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class SubscriptionsViewModel
    @Inject
    constructor(
        private val repo: PodcastSubscriptionsRepo,
        private val unsubscribeUseCase: UnsubscribePodcastUseCase,
        private val prefs: InstancePrefs,
        private val searchEpisodesUseCase: SearchSubscriptionEpisodesUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<SubscriptionsUiState>(SubscriptionsUiState.Loading)
        val ui: StateFlow<SubscriptionsUiState> = _ui.asStateFlow()

        private val _message = MutableStateFlow<String?>(null)
        val message: StateFlow<String?> = _message.asStateFlow()

        private val _searchUi = MutableStateFlow(SubscriptionSearchUi())
        val searchUi: StateFlow<SubscriptionSearchUi> = _searchUi.asStateFlow()

        /** Orden manual persistido en este dispositivo. */
        val manualOrder: StateFlow<List<Long>> =
            prefs.podcastSubscriptionOrder.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

        private var searchJob: Job? = null
        private var searchGeneration = 0

        init {
            refresh()
            viewModelScope.launch {
                repo.subscriptions.collect { items ->
                    if (_ui.value !is SubscriptionsUiState.Loading) {
                        publishList(items)
                    }
                }
            }
        }

        fun refresh() {
            viewModelScope.launch {
                _ui.value = SubscriptionsUiState.Loading
                when (val r = repo.list()) {
                    is ApiResult.Ok -> publishList(r.value)
                    is ApiResult.Err -> _ui.value = SubscriptionsUiState.Error(r.error.userMessage())
                }
            }
        }

        /** Filtro local mientras se escribe en «Podcasts» (FR-060). */
        fun setSearchTerm(term: String) {
            _searchUi.update { it.copy(term = term) }
            if (term.isBlank()) clearSearchResults()
        }

        /** Cambia de ámbito sin perder el término compartido (FR-070). */
        fun setSearchScope(scope: SubscriptionSearchScope) {
            if (_searchUi.value.scope == scope) return
            clearSearchResults()
            _searchUi.update { it.copy(scope = scope) }
        }

        /** Confirma la búsqueda de episodios (FR-061). */
        fun searchEpisodes() {
            val term = _searchUi.value.term.trim()
            if (term.isEmpty()) {
                clearSearchResults()
                return
            }
            launchSearch(term)
        }

        /** Carga la página siguiente sin repetir episodios (FR-062). */
        fun loadMoreEpisodes() {
            val current = _searchUi.value
            if (!current.showResults) return
            if (current.loading || current.loadingMore) return
            if (!current.hasMore || current.submittedTerm.isBlank()) return
            val generation = searchGeneration
            _searchUi.update { it.copy(loadingMore = true, error = null) }
            viewModelScope.launch {
                when (
                    val r =
                        searchEpisodesUseCase(
                            current.submittedTerm,
                            current.results.size,
                            SEARCH_PAGE_SIZE,
                        )
                ) {
                    is ApiResult.Ok ->
                        if (generation == searchGeneration) {
                            _searchUi.update { state ->
                                val merged =
                                    (state.results + r.value.items.map { it.toSubscriptionResult() })
                                        .distinctBy { it.episode.id }
                                state.copy(
                                    results = merged,
                                    hasMore = r.value.pagination.hasMore,
                                    loadingMore = false,
                                )
                            }
                        }
                    is ApiResult.Err ->
                        if (generation == searchGeneration) {
                            _searchUi.update {
                                it.copy(loadingMore = false, error = r.error.userMessage())
                            }
                        }
                }
            }
        }

        /** Atrás desde los resultados: vuelve a la lista y conserva el término (FR-071). */
        fun closeEpisodeSearch() {
            clearSearchResults()
            _searchUi.update { it.copy(scope = SubscriptionSearchScope.PODCASTS) }
        }

        /** Mantiene el estado de escucha de un resultado tras marcarlo (FR-063). */
        fun onEpisodeProgressChanged(episodeId: Long) {
            _searchUi.update { state ->
                if (state.results.none { it.episode.id == episodeId }) return@update state
                state.copy(
                    results =
                        state.results.map { result ->
                            if (result.episode.id != episodeId) {
                                result
                            } else {
                                val current = result.episode.progress
                                val completed = current?.completed != true
                                val duration = current?.durationSec ?: result.episode.durationSec ?: 0
                                result.copy(
                                    episode =
                                        result.episode.copy(
                                            progress =
                                                (current ?: PodcastEpisodeProgress()).copy(
                                                    positionSec = if (completed) duration else 0,
                                                    completed = completed,
                                                    completedAt =
                                                        if (completed) System.currentTimeMillis() else null,
                                                ),
                                        ),
                                )
                            }
                        },
                )
            }
        }

        fun unsubscribe(podcastId: Long) {
            viewModelScope.launch {
                when (val r = unsubscribeUseCase(podcastId)) {
                    is ApiResult.Ok -> {
                        _message.value = "Baja de la suscripción."
                        removePodcastFromSearch(podcastId)
                    }
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        fun clearMessage() {
            _message.value = null
        }

        /** Guarda el orden manual arrastrado (solo local, como en la web no hay servidor). */
        fun saveManualOrder(ids: List<Long>) {
            viewModelScope.launch { prefs.setPodcastSubscriptionOrder(ids) }
        }

        private fun publishList(items: List<PodcastSummary>) {
            _ui.value =
                if (items.isEmpty()) {
                    clearSearchResults()
                    SubscriptionsUiState.Empty
                } else {
                    SubscriptionsUiState.Content(items)
                }
        }

        private fun launchSearch(term: String) {
            searchJob?.cancel()
            val generation = ++searchGeneration
            searchJob =
                viewModelScope.launch {
                    _searchUi.update {
                        it.copy(
                            submittedTerm = term,
                            loading = true,
                            loadingMore = false,
                            error = null,
                            showResults = true,
                            results = emptyList(),
                            hasMore = false,
                        )
                    }
                    when (val r = searchEpisodesUseCase(term, 0, SEARCH_PAGE_SIZE)) {
                        is ApiResult.Ok ->
                            if (generation == searchGeneration) {
                                _searchUi.update {
                                    it.copy(
                                        results =
                                            r.value.items
                                                .map { item -> item.toSubscriptionResult() }
                                                .distinctBy { it.episode.id },
                                        hasMore = r.value.pagination.hasMore,
                                        loading = false,
                                    )
                                }
                            }
                        is ApiResult.Err ->
                            if (generation == searchGeneration) {
                                _searchUi.update {
                                    it.copy(
                                        results = emptyList(),
                                        hasMore = false,
                                        loading = false,
                                        error = r.error.userMessage(),
                                    )
                                }
                            }
                    }
                }
        }

        private fun clearSearchResults() {
            searchJob?.cancel()
            searchJob = null
            searchGeneration++
            _searchUi.update { state ->
                if (!state.showResults && state.results.isEmpty()) {
                    if (state.submittedTerm.isEmpty() && state.error == null) return@update state
                }
                state.copy(
                    submittedTerm = "",
                    results = emptyList(),
                    loading = false,
                    loadingMore = false,
                    hasMore = false,
                    error = null,
                    showResults = false,
                )
            }
        }

        private fun removePodcastFromSearch(podcastId: Long) {
            _searchUi.update { state ->
                state.copy(results = state.results.filterNot { it.podcast.id == podcastId })
            }
            val current = _searchUi.value
            if (current.showResults && current.results.isEmpty() && current.hasMore) {
                loadMoreEpisodes()
            }
        }
    }
