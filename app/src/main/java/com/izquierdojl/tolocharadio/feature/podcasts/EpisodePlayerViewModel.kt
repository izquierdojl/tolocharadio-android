package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.ProgressRepo
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.domain.podcasts.DismissEpisodeUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.EPISODE_SKIP_BACK_MS
import com.izquierdojl.tolocharadio.domain.podcasts.EPISODE_SKIP_FORWARD_MS
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackController
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlayerEvent
import com.izquierdojl.tolocharadio.domain.podcasts.ListeningTimeTracker
import com.izquierdojl.tolocharadio.domain.podcasts.LoadEpisodeProgressUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.NextEpisodeUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.ResolveEpisodePlaybackUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.ResumeSource
import com.izquierdojl.tolocharadio.domain.podcasts.resumePoint
import com.izquierdojl.tolocharadio.feature.player.VoiceBoostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Velocidades del reproductor de podcast (paridad web). */
val EPISODE_RATES = listOf(1f, 1.25f, 1.5f, 2f)

/** Reporte periódico de progreso (research D4, ms). */
private const val PROGRESS_REPORT_MS = 30_000L

/** Milisegundos por segundo. */
private const val MS_PER_SEC = 1_000L

/** UI del reproductor de episodios (US3, FR-014–FR-018). */
data class EpisodePlayerUi(
    val episode: PodcastEpisode? = null,
    val podcast: PodcastSummary? = null,
    val playing: Boolean = false,
    val loading: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val rate: Float = 1f,
    val downloaded: Boolean = false,
    val recovering: Boolean = false,
    val message: String? = null,
)

/**
 * Orquesta el VOD: resuelve la fuente (local/firmada), pilota el
 * controlador, informa el progreso y auto-avanza al terminar (FR-016).
 */
@HiltViewModel
@Suppress("TooManyFunctions", "LongParameterList")
class EpisodePlayerViewModel
    @Inject
    constructor(
        private val controller: EpisodePlaybackController,
        private val progress: ProgressRepo,
        private val queue: QueueRepo,
        private val resolve: ResolveEpisodePlaybackUseCase,
        private val next: NextEpisodeUseCase,
        private val dismiss: DismissEpisodeUseCase,
        private val loadProgress: LoadEpisodeProgressUseCase,
        private val voiceBoost: VoiceBoostController,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(EpisodePlayerUi())
        val ui: StateFlow<EpisodePlayerUi> = _ui.asStateFlow()

        /** Preferencia del potenciador de voz (spec 0047, US4). */
        val voiceBoostEnabled: StateFlow<Boolean> = voiceBoost.enabled

        private var sessionId: String? = null
        private var sequence: Long = 0
        private var ticker: Job? = null
        private val listening = ListeningTimeTracker()

        init {
            viewModelScope.launch {
                controller.state.collect { state ->
                    val current = _ui.value
                    _ui.value =
                        when (state) {
                            is EpisodePlaybackController.State.Idle ->
                                if (current.episode == null) {
                                    current
                                } else {
                                    // Idle = el controlador ya no tiene el episodio
                                    // (radio, temporizador o parada externa). Se cierra
                                    // la tarjeta para no reportar la posición 0.
                                    EpisodePlayerUi(rate = current.rate)
                                }
                            is EpisodePlaybackController.State.Loading ->
                                current.copy(loading = true)
                            is EpisodePlaybackController.State.WaitingForNetwork ->
                                current.copy(
                                    loading = false,
                                    playing = false,
                                    positionMs = state.positionMs,
                                    message = null,
                                    recovering = true,
                                )
                            is EpisodePlaybackController.State.Reconnecting ->
                                current.copy(
                                    loading = false,
                                    playing = false,
                                    positionMs = state.positionMs,
                                    message = null,
                                    recovering = true,
                                )
                            is EpisodePlaybackController.State.Active -> {
                                listening.onProgress(state.positionMs, state.playing)
                                if (state.playing && current.recovering) startTicker(state.episode.id)
                                current.copy(
                                    playing = state.playing,
                                    loading = false,
                                    positionMs = state.positionMs,
                                    durationMs = state.durationMs,
                                    rate = state.rate,
                                    downloaded = state.local,
                                    recovering = false,
                                )
                            }
                            is EpisodePlaybackController.State.Error ->
                                current.copy(
                                    loading = false,
                                    playing = false,
                                    positionMs = state.positionMs,
                                    message = state.message,
                                    recovering = false,
                                )
                        }
                }
            }
            viewModelScope.launch {
                controller.events.collect { event ->
                    when (event) {
                        is EpisodePlayerEvent.Ended -> onEnded(event.episodeId, event.durationSec)
                        is EpisodePlayerEvent.Failed ->
                            _ui.value = _ui.value.copy(message = "No se pudo reproducir el episodio.")
                    }
                }
            }
        }

        /** Reproduce un episodio desde el inicio o su progreso guardado. */
        fun play(
            episode: PodcastEpisode,
            podcast: PodcastSummary?,
        ) {
            viewModelScope.launch {
                _ui.value =
                    EpisodePlayerUi(episode = episode, podcast = podcast, loading = true, rate = _ui.value.rate)
                val embedded = episode.progress
                val remote =
                    when (val r = loadProgress(episode.id)) {
                        is ApiResult.Ok -> r.value
                        is ApiResult.Err -> null
                    }
                val fresh = remote ?: embedded
                val source =
                    when {
                        remote != null -> ResumeSource.REMOTE
                        embedded != null -> ResumeSource.EMBEDDED
                        else -> ResumeSource.NONE
                    }
                val resume = resumePoint(episode.id, fresh, episode.durationSec, source)
                startPlayback(episode, podcast, startMs = resume.positionMs)
            }
        }

        fun toggle() {
            val id = _ui.value.episode?.id ?: return
            if (_ui.value.playing) {
                ticker?.cancel()
                controller.pause()
                report(id)
            } else {
                controller.resume()
                startTicker(id)
            }
        }

        fun seekTo(positionMs: Long) {
            controller.seekTo(positionMs)
            report(_ui.value.episode?.id ?: return)
        }

        fun skipBack() = seekTo((_ui.value.positionMs - EPISODE_SKIP_BACK_MS).coerceAtLeast(0))

        fun skipForward() = seekTo(_ui.value.positionMs + EPISODE_SKIP_FORWARD_MS)

        fun cycleRate() {
            val nextRate = EPISODE_RATES.firstOrNull { it > _ui.value.rate } ?: EPISODE_RATES.first()
            controller.setRate(nextRate)
        }

        /** Activa/desactiva el potenciador de voz y lo persiste (US4). */
        fun setVoiceBoost(enabled: Boolean) = voiceBoost.setEnabled(enabled)

        /** Parada del usuario con reporte final; limpia la tarjeta inferior. */
        fun stop() {
            val id = _ui.value.episode?.id
            val durationMs = _ui.value.durationMs
            val position = controller.stop()
            ticker?.cancel()
            _ui.value = EpisodePlayerUi(rate = _ui.value.rate)
            if (id == null) return
            viewModelScope.launch {
                val result = reportProgress(id, position, completed = false, durationMs = durationMs)
                // El servidor auto-completa cerca del final; si lo hizo, sale de la cola.
                if (result?.completed == true) queue.remove(id)
            }
        }

        /** Descarta de "continuar escuchando" sin completar (FR-016). */
        fun dismissFromContinue(episodeId: Long) {
            viewModelScope.launch {
                when (val r = dismiss(episodeId)) {
                    is ApiResult.Ok -> _ui.value = _ui.value.copy(message = "Episodio descartado.")
                    is ApiResult.Err -> _ui.value = _ui.value.copy(message = r.error.userMessage())
                }
            }
        }

        /** Keepalive al pasar a segundo plano/cierre (research D4). */
        fun onAppBackground() {
            val id = _ui.value.episode?.id ?: return
            viewModelScope.launch {
                val session = sessionId ?: return@launch
                val positionMs = controller.positionMs()
                listening.onProgress(positionMs, _ui.value.playing)
                val positionSec = positionMs / MS_PER_SEC
                sequence += 1
                progress.keepalive(
                    id,
                    session,
                    sequence,
                    positionSec,
                    durationSec = durationSec(),
                    listenedMs = listening.totalMs,
                )
            }
        }

        fun clearMessage() {
            _ui.value = _ui.value.copy(message = null)
        }

        private suspend fun startPlayback(
            episode: PodcastEpisode,
            podcast: PodcastSummary?,
            startMs: Long,
        ) {
            when (val r = resolve(episode)) {
                is ApiResult.Ok -> {
                    listening.reset()
                    sessionId = UUID.randomUUID().toString()
                    sequence = 0
                    controller.play(r.value, episode, podcast, startMs, _ui.value.rate)
                    startTicker(episode.id)
                }
                is ApiResult.Err ->
                    _ui.value = _ui.value.copy(loading = false, message = r.error.userMessage())
            }
        }

        private fun onEnded(
            episodeId: Long,
            durationSec: Long,
        ) {
            // Se captura antes de suspender: el estado Idle limpia la tarjeta.
            val podcast = _ui.value.podcast
            viewModelScope.launch {
                ticker?.cancel()
                complete(episodeId, durationSec)
                // Paridad con la web: el episodio terminado sale de la cola antes de avanzar.
                queue.remove(episodeId)
                val nextId = next(episodeId)
                if (nextId == null) {
                    _ui.value = EpisodePlayerUi(rate = _ui.value.rate)
                    return@launch
                }
                val item = queue.queue.value.items.firstOrNull { it.id == nextId } ?: return@launch
                val sameShow = podcast?.takeIf { it.id == item.podcastId }
                play(item, sameShow)
            }
        }

        private fun startTicker(episodeId: Long) {
            ticker?.cancel()
            ticker =
                viewModelScope.launch {
                    while (true) {
                        delay(PROGRESS_REPORT_MS)
                        if (_ui.value.episode?.id != episodeId || !_ui.value.playing) return@launch
                        report(episodeId)
                    }
                }
        }

        private fun report(
            episodeId: Long,
            positionMs: Long = controller.positionMs(),
            completed: Boolean = false,
            durationMs: Long = _ui.value.durationMs,
        ) {
            viewModelScope.launch { reportProgress(episodeId, positionMs, completed, durationMs) }
        }

        /** Reporta y devuelve el progreso resultante (null si no hay sesión o falla). */
        private suspend fun reportProgress(
            episodeId: Long,
            positionMs: Long,
            completed: Boolean,
            durationMs: Long,
        ): PodcastEpisodeProgress? {
            val session = sessionId ?: return null
            listening.synchronize(positionMs)
            sequence += 1
            val positionSec = (if (completed) durationMs else positionMs) / MS_PER_SEC
            return when (
                val r =
                    progress.report(
                        episodeId = episodeId,
                        sessionId = session,
                        sequence = sequence,
                        positionSec = positionSec,
                        durationSec = durationMs.takeIf { it > 0L }?.div(MS_PER_SEC),
                        listenedMs = listening.totalMs,
                        completed = if (completed) true else null,
                    )
            ) {
                is ApiResult.Ok -> r.value
                is ApiResult.Err -> null
            }
        }

        private suspend fun complete(
            episodeId: Long,
            durationSec: Long,
        ) {
            reportProgress(
                episodeId = episodeId,
                positionMs = durationSec * MS_PER_SEC,
                completed = true,
                durationMs = durationSec * MS_PER_SEC,
            )
        }

        /** Duración conocida del episodio en segundos (null si aún no cargó). */
        private fun durationSec(): Long? = (_ui.value.durationMs / MS_PER_SEC).takeIf { it > 0 }
    }
