package com.izquierdojl.tolocharadio.feature.podcasts.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import com.izquierdojl.tolocharadio.data.repo.QueuePosition
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.data.repo.QueueState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de la cola (US4, FR-019–FR-022). */
sealed interface QueueUiState {
    data object Loading : QueueUiState

    data object Empty : QueueUiState

    data class Content(
        val items: List<PodcastEpisode>,
        val revision: Long,
        val limit: Int,
        val playingId: Long? = null,
    ) : QueueUiState

    data class Error(val message: String) : QueueUiState
}

/** Cola de reproducción con revisión optimista y vaciado seguro (US4). */
@HiltViewModel
@Suppress("TooManyFunctions")
class QueueViewModel
    @Inject
    constructor(
        private val repo: QueueRepo,
        private val subscriptions: PodcastSubscriptionsRepo,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<QueueUiState>(QueueUiState.Loading)
        val ui: StateFlow<QueueUiState> = _ui.asStateFlow()

        private val _message = MutableStateFlow<String?>(null)
        val message: StateFlow<String?> = _message.asStateFlow()

        private var playingId: Long? = null

        /** Orden pendiente de arrastre (se confirma con [commitOrder]). */
        private var pendingOrder: List<Long>? = null

        init {
            refresh()
            viewModelScope.launch {
                repo.queue.collect { render(it, playingId) }
            }
        }

        fun refresh() {
            viewModelScope.launch {
                _ui.value = QueueUiState.Loading
                when (val r = repo.load()) {
                    is ApiResult.Ok -> render(r.value, playingId)
                    is ApiResult.Err -> _ui.value = QueueUiState.Error(r.error.userMessage())
                }
            }
        }

        /** Episodio en curso para ocultarlo de pendientes (lo informa la pantalla). */
        fun setPlayingId(id: Long?) {
            playingId = id
            render(repo.queue.value, id)
        }

        fun addToQueue(
            episodeId: Long,
            position: QueuePosition,
        ) {
            viewModelScope.launch {
                when (val r = repo.add(episodeId, position)) {
                    is ApiResult.Ok -> render(r.value, playingId)
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        fun remove(episodeId: Long) {
            viewModelScope.launch {
                when (val r = repo.remove(episodeId)) {
                    is ApiResult.Ok -> render(r.value, playingId)
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        /** Vacía los pendientes sin detener el episodio en curso (FR-021). */
        fun clear(keepEpisodeId: Long?) {
            viewModelScope.launch {
                when (val r = repo.clear(keepEpisodeId)) {
                    is ApiResult.Ok -> render(r.value, playingId)
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        /** Mueve una tarjeta en el orden visible (arrastre); guarda con [commitOrder]. */
        fun moveItem(
            from: Int,
            to: Int,
        ) {
            val base = pendingOrder ?: repo.queue.value.items.filterNot { it.id == playingId }.map { it.id }
            if (from !in base.indices || to !in base.indices || from == to) return
            val moved = base.toMutableList()
            val item = moved.removeAt(from)
            moved.add(to, item)
            pendingOrder = moved
            render(repo.queue.value, playingId, visibleOverride = moved)
        }

        /** Confirma el orden arrastrado (PUT con revisión, FR-022). */
        fun commitOrder() {
            val order = pendingOrder ?: return
            pendingOrder = null
            reorder(order, playingId)
        }

        /** Reordena visibles conservando el hueco del episodio en curso. */
        fun reorder(
            visibleOrder: List<Long>,
            hiddenEpisodeId: Long?,
        ) {
            viewModelScope.launch {
                when (val r = repo.reorder(visibleOrder, hiddenEpisodeId)) {
                    is ApiResult.Ok -> render(r.value, playingId)
                    is ApiResult.Err -> {
                        _message.value =
                            if (r.error is DomainError.Conflict) {
                                "La cola cambió en otro dispositivo. Vista actualizada."
                            } else {
                                r.error.userMessage()
                            }
                        render(repo.queue.value, playingId)
                    }
                }
            }
        }

        /** Programa del episodio para la reproducción (carátula/artista). */
        fun podcastFor(episode: PodcastEpisode): PodcastSummary? =
            subscriptions.subscriptions.value.firstOrNull { it.id == episode.podcastId }

        fun clearMessage() {
            _message.value = null
        }

        private fun render(
            state: QueueState,
            playingId: Long?,
            visibleOverride: List<Long>? = null,
        ) {
            val visible =
                if (visibleOverride != null) {
                    val byId = state.items.associateBy { it.id }
                    visibleOverride.mapNotNull { byId[it] }
                } else {
                    state.items.filterNot { it.id == playingId }
                }
            _ui.value =
                if (visible.isEmpty()) {
                    QueueUiState.Empty
                } else {
                    QueueUiState.Content(visible, state.revision, state.limit, playingId)
                }
        }
    }
