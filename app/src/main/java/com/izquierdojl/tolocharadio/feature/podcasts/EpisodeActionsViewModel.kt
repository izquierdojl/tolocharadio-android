package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.repo.DownloadsRepo
import com.izquierdojl.tolocharadio.data.repo.EpisodeFavoritesRepo
import com.izquierdojl.tolocharadio.data.repo.ProgressRepo
import com.izquierdojl.tolocharadio.data.repo.QueuePosition
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.data.repo.SystemRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Milisegundos por segundo para reportar tiempo escuchado. */
private const val MS_PER_SEC = 1_000L

/**
 * Acciones compartidas sobre episodios (filas, ficha y portada):
 * favorito, cola, descarga y descarte. Un ViewModel por pantalla
 * no: se comparte a ámbito de actividad para estado coherente.
 */
@HiltViewModel
class EpisodeActionsViewModel
    @Inject
    constructor(
        private val favorites: EpisodeFavoritesRepo,
        private val queue: QueueRepo,
        private val downloads: DownloadsRepo,
        private val progress: ProgressRepo,
        private val system: SystemRepo,
    ) : ViewModel() {
        val favoriteIds: StateFlow<Set<Long>> = favorites.favoriteIds
        val downloadRecords = downloads.records

        private val _downloadsEnabled = MutableStateFlow(false)
        val downloadsEnabled: StateFlow<Boolean> = _downloadsEnabled.asStateFlow()

        private val _message = MutableStateFlow<String?>(null)
        val message: StateFlow<String?> = _message.asStateFlow()

        private val _progressChanged = MutableSharedFlow<Long>(extraBufferCapacity = 1)

        /** Emite el id del episodio cuyo estado reproducido/pendiente cambió. */
        val progressChanged: SharedFlow<Long> = _progressChanged.asSharedFlow()

        init {
            viewModelScope.launch { favorites.list() }
            viewModelScope.launch { downloads.refresh() }
            viewModelScope.launch {
                _downloadsEnabled.value =
                    when (val r = system.podcastCapabilities()) {
                        is ApiResult.Ok -> r.value.downloads
                        is ApiResult.Err -> false
                    }
            }
        }

        fun toggleFavorite(episode: PodcastEpisode) {
            viewModelScope.launch {
                when (val r = favorites.toggle(episode.id, episode.isFavorite)) {
                    is ApiResult.Ok -> Unit
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        fun addToQueue(
            episodeId: Long,
            position: QueuePosition,
        ) {
            viewModelScope.launch {
                when (val r = queue.add(episodeId, position)) {
                    is ApiResult.Ok ->
                        _message.value =
                            if (position == QueuePosition.NEXT) {
                                "Suena a continuación."
                            } else {
                                "Añadido al final de la cola."
                            }
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        fun download(episode: PodcastEpisode) {
            viewModelScope.launch {
                when (val r = downloads.download(episode)) {
                    is ApiResult.Ok -> _message.value = "Descarga iniciada."
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        fun dismiss(episodeId: Long) {
            viewModelScope.launch {
                when (val r = progress.dismiss(episodeId)) {
                    is ApiResult.Ok -> _message.value = "Episodio descartado."
                    is ApiResult.Err -> _message.value = r.error.userMessage()
                }
            }
        }

        /**
         * Alterna reproducido/pendiente. Un episodio completado se
         * desmarca con un informe explícito `completed=false` en la
         * posición cero; el pendiente se marca completado con
         * `completed=true` en su duración.
         */
        fun togglePlayed(episode: PodcastEpisode) {
            val wasCompleted = episode.progress?.completed == true
            viewModelScope.launch {
                val duration = episode.durationSec?.coerceAtLeast(0)
                val position = if (wasCompleted) 0 else duration ?: 0
                val result =
                    progress.report(
                        episodeId = episode.id,
                        sessionId = UUID.randomUUID().toString(),
                        sequence = 1,
                        positionSec = position,
                        durationSec = duration,
                        listenedMs = if (wasCompleted) 0 else (duration ?: 0) * MS_PER_SEC,
                        completed = !wasCompleted,
                    )
                when (result) {
                    is ApiResult.Ok -> {
                        if (wasCompleted && result.value.completed) {
                            _message.value =
                                "El servidor no ha permitido marcar el episodio como pendiente."
                        } else {
                            _message.value =
                                if (wasCompleted) {
                                    "Marcado como pendiente."
                                } else {
                                    "Marcado como reproducido."
                                }
                            _progressChanged.tryEmit(episode.id)
                        }
                    }
                    is ApiResult.Err -> _message.value = result.error.userMessage()
                }
            }
        }

        fun clearMessage() {
            _message.value = null
        }
    }
