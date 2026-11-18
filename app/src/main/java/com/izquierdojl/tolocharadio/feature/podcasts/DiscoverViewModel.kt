package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import com.izquierdojl.tolocharadio.domain.podcasts.ImportOpmlUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.SubscribePodcastUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.UnsubscribePodcastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de Descubrir: búsqueda, alta manual e importación (US2). */
data class DiscoverUi(
    val query: String = "",
    val searching: Boolean = false,
    val results: List<PodcastSearchResult> = emptyList(),
    val subscribedFeedUrls: Set<String> = emptySet(),
    val message: String? = null,
)

/** Descubrir programas y suscribirse (US2, FR-006–FR-010). */
@HiltViewModel
class DiscoverViewModel
    @Inject
    constructor(
        private val repo: PodcastSubscriptionsRepo,
        private val subscribeUseCase: SubscribePodcastUseCase,
        private val unsubscribeUseCase: UnsubscribePodcastUseCase,
        private val importOpmlUseCase: ImportOpmlUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(DiscoverUi())
        val ui: StateFlow<DiscoverUi> = _ui.asStateFlow()

        init {
            viewModelScope.launch { repo.list() }
            viewModelScope.launch {
                repo.subscribedFeedUrls.collect { urls ->
                    _ui.value = _ui.value.copy(subscribedFeedUrls = urls)
                }
            }
        }

        fun onQueryChange(query: String) {
            _ui.value = _ui.value.copy(query = query, message = null)
        }

        /** Busca en el directorio; el texto vacío solo avisa (FR-006). */
        fun search() {
            val query = _ui.value.query
            if (query.isBlank()) {
                _ui.value = _ui.value.copy(message = "Escribe un texto para buscar.")
                return
            }
            _ui.value = _ui.value.copy(searching = true, message = null)
            viewModelScope.launch {
                when (val r = repo.search(query)) {
                    is ApiResult.Ok ->
                        _ui.value = _ui.value.copy(searching = false, results = r.value)
                    is ApiResult.Err ->
                        _ui.value = _ui.value.copy(searching = false, message = r.error.userMessage())
                }
            }
        }

        /** Alta manual de un feed RSS (FR-007). */
        fun resolveManual(feedUrl: String) {
            viewModelScope.launch {
                when (val r = repo.resolveFeed(feedUrl)) {
                    is ApiResult.Ok -> {
                        repo.list()
                        _ui.value = _ui.value.copy(message = "Suscrito a ${r.value.podcast.title}.")
                    }
                    is ApiResult.Err ->
                        _ui.value = _ui.value.copy(message = r.error.userMessage())
                }
            }
        }

        /** Suscribe o da de baja desde un resultado del directorio. */
        fun toggleSubscribe(result: PodcastSearchResult) {
            viewModelScope.launch {
                if (result.feedUrl in _ui.value.subscribedFeedUrls) {
                    val id = repo.subscriptions.value.firstOrNull { it.feedUrl == result.feedUrl }?.id
                    if (id == null) {
                        repo.list()
                        return@launch
                    }
                    when (val r = unsubscribeUseCase(id)) {
                        is ApiResult.Ok -> _ui.value = _ui.value.copy(message = "Baja de la suscripción.")
                        is ApiResult.Err -> _ui.value = _ui.value.copy(message = r.error.userMessage())
                    }
                } else {
                    when (val r = subscribeUseCase.byFeedUrl(result.feedUrl)) {
                        is ApiResult.Ok ->
                            _ui.value = _ui.value.copy(message = "Suscrito a ${r.value.title}.")
                        is ApiResult.Err -> _ui.value = _ui.value.copy(message = r.error.userMessage())
                    }
                }
            }
        }

        /** Importa suscripciones desde el texto OPML (FR-010). */
        fun importOpml(xml: String) {
            viewModelScope.launch {
                when (val r = importOpmlUseCase(xml)) {
                    is ApiResult.Ok ->
                        _ui.value =
                            _ui.value.copy(message = "Importadas ${r.value.imported} suscripciones.")
                    is ApiResult.Err ->
                        _ui.value = _ui.value.copy(message = r.error.userMessage())
                }
            }
        }

        /** Texto OPML para exportar (null si falla, con mensaje). */
        suspend fun exportOpmlText(): String? {
            return when (val r = repo.exportOpml()) {
                is ApiResult.Ok -> r.value
                is ApiResult.Err -> {
                    _ui.value = _ui.value.copy(message = r.error.userMessage())
                    null
                }
            }
        }

        fun clearMessage() {
            _ui.value = _ui.value.copy(message = null)
        }

        /** Aviso directo (p. ej. fallo al guardar el OPML exportado). */
        fun showMessage(text: String) {
            _ui.value = _ui.value.copy(message = text)
        }
    }
