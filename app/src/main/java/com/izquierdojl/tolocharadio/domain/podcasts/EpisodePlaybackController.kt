package com.izquierdojl.tolocharadio.domain.podcasts

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.network.ConnectivityMonitor
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.di.ApplicationScope
import com.izquierdojl.tolocharadio.domain.playback.RecoveryPolicy
import com.izquierdojl.tolocharadio.feature.player.PlaybackClock
import com.izquierdojl.tolocharadio.feature.player.PlaybackEngine
import com.izquierdojl.tolocharadio.feature.player.PlaybackErrorClassifier
import com.izquierdojl.tolocharadio.feature.player.StationMediaItemFactory
import com.izquierdojl.tolocharadio.feature.player.VoiceBoostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Intervalo de muestreo de posición del episodio en curso (ms). */
private const val POSITION_POLL_MS = 1_000L

/** Milisegundos por segundo. */
private const val MS_PER_SEC = 1_000L

/** Cadencia de comprobación del player durante una reconexión (ms). */
private const val READY_POLL_MS = 100L

/** Eventos del controlador VOD hacia el ViewModel. */
sealed interface EpisodePlayerEvent {
    data class Ended(val episodeId: Long, val durationSec: Long) : EpisodePlayerEvent

    data class Failed(val episodeId: Long, val message: String) : EpisodePlayerEvent
}

/**
 * Pilota el reproductor para episodios (research D10): fichero finito
 * con URL firmada o archivo local, sin reconexión live.
 *
 * Un fallo de red en streaming no es terminal: el controlador espera a que
 * vuelva la conectividad y reintenta en la última posición conocida con el
 * presupuesto de [RecoveryPolicy] (bug 0057).
 *
 * Emite al receptor cuando hay sesión Cast (misma política que la
 * radio, T042); si se pierde la conexión, reanuda en local en la
 * última posición conocida. La radio y los episodios nunca suenan a
 * la vez: quien arranca corta al otro (PlayerViewModel ↔ [stopExternal]).
 */
@Singleton
@Suppress("TooManyFunctions", "LongParameterList")
class EpisodePlaybackController
    @Inject
    constructor(
        private val engine: PlaybackEngine,
        private val cast: CastPlayerManager,
        private val mediaItems: StationMediaItemFactory,
        private val voiceBoost: VoiceBoostController,
        private val connectivity: ConnectivityMonitor,
        private val clock: PlaybackClock,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        /** Estado observable del VOD. */
        sealed interface State {
            data object Idle : State

            data class Loading(val episode: PodcastEpisode) : State

            data class Active(
                val episode: PodcastEpisode,
                val podcast: PodcastSummary?,
                val playing: Boolean,
                val positionMs: Long,
                val durationMs: Long,
                val rate: Float,
                val local: Boolean,
                val casting: Boolean,
            ) : State

            /** Sin red: espera indefinida (no consume intentos) hasta recuperarla. */
            data class WaitingForNetwork(
                val episode: PodcastEpisode,
                val podcast: PodcastSummary?,
                val positionMs: Long,
            ) : State

            /** Con red: reconectando en la posición guardada. */
            data class Reconnecting(
                val episode: PodcastEpisode,
                val podcast: PodcastSummary?,
                val positionMs: Long,
                val attempt: Int,
                val maxAttempts: Int,
            ) : State

            data class Error(
                val episode: PodcastEpisode,
                val podcast: PodcastSummary?,
                val positionMs: Long,
                val rate: Float,
                val message: String,
            ) : State
        }

        private val _state = MutableStateFlow<State>(State.Idle)
        val state: StateFlow<State> = _state.asStateFlow()

        private val _events = MutableSharedFlow<EpisodePlayerEvent>(extraBufferCapacity = 1)
        val events: SharedFlow<EpisodePlayerEvent> = _events.asSharedFlow()

        private val policy = RecoveryPolicy()
        private var pollJob: Job? = null
        private var recoveryJob: Job? = null
        private var finalized = false
        private var userPaused = false
        private var listenedPlayer: Player? = null
        private var lastSource: EpisodePlaybackSource? = null
        private var lastEpisode: PodcastEpisode? = null
        private var lastPodcast: PodcastSummary? = null
        private var lastRate: Float = 1f
        private var lastPositionMs: Long = 0L
        private var pendingReload = false

        /** `true` cuando hay un episodio cargado (la radio debe ignorar eventos). */
        fun isEpisodeActive(): Boolean = _state.value !is State.Idle

        private val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) onEnded()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (finalized || userPaused || recoveryJob?.isActive == true) return
                    val episode = lastEpisode ?: return
                    // Sin PII: solo código y tipo de causa para diagnóstico.
                    Log.e(
                        "EpisodePlayback",
                        "code=${error.errorCode} cause=${error.cause?.javaClass?.simpleName}",
                    )
                    val position = knownPosition()
                    if (!shouldRecover(error)) {
                        fail(episode, position, "No se pudo reproducir el episodio.")
                        return
                    }
                    startRecovery(position)
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                        userPaused = true
                        cancelRecovery()
                        markPaused()
                        reattach()
                    }
                }
            }

        init {
            // El player exige el hilo main (Media3): tanto la escucha de
            // desconexiones Cast como el muestreo corren confinados a Main.
            scope.launch(Dispatchers.Main.immediate) {
                cast.connectionState.collect {
                    val current = _state.value as? State.Active ?: return@collect
                    if (!cast.isCastConnected && current.casting) {
                        resumeLocally(current)
                    }
                }
            }
        }

        /** Reproduce desde `startMs` con la velocidad dada. */
        @OptIn(UnstableApi::class)
        fun play(
            source: EpisodePlaybackSource,
            episode: PodcastEpisode,
            podcast: PodcastSummary?,
            startMs: Long,
            rate: Float,
        ) {
            finalized = false
            userPaused = false
            pendingReload = false
            cancelRecovery()
            stopTicker()
            lastSource = source
            lastEpisode = episode
            lastPodcast = podcast
            lastRate = rate
            lastPositionMs = startMs
            _state.value = State.Loading(episode)
            val casting = cast.isCastConnected
            voiceBoost.setEpisodeActive(true)
            voiceBoost.setCastConnected(casting)
            if (casting) {
                cast.connectToEpisode(source.uri, episode.title, podcast?.title, startMs)
                attach(activePlayer())
            } else {
                loadLocally(source, episode, podcast, startMs)
            }
            applyRate(rate, casting)
            activePlayer().playWhenReady = true
            _state.value =
                State.Active(episode, podcast, playing = true, startMs, durationOf(), rate, source.local, casting)
            startTicker()
        }

        fun pause() {
            userPaused = true
            cancelRecovery()
            markPaused()
            activePlayer().playWhenReady = false
            reattach()
        }

        /**
         * Reanuda. Si el episodio quedó en [State.Error], reintenta desde la
         * última posición conocida con la misma fuente (botón play = reintentar).
         */
        fun resume() {
            userPaused = false
            val source = lastSource
            val episode = lastEpisode
            val failed = _state.value as? State.Error
            val retry = pendingReload || failed != null
            if (retry && source != null && episode != null) {
                val position = failed?.positionMs ?: knownPosition()
                play(source, episode, failed?.podcast ?: lastPodcast, position, failed?.rate ?: lastRate)
                return
            }
            reattach()
            activePlayer().playWhenReady = true
            (_state.value as? State.Active)?.let { _state.value = it.copy(playing = true) }
        }

        fun seekTo(positionMs: Long) {
            val position = positionMs.coerceAtLeast(0)
            lastPositionMs = position
            activePlayer().seekTo(position)
            syncPosition()
        }

        fun skip(deltaMs: Long) {
            seekTo(activePlayer().currentPosition + deltaMs)
        }

        /**
         * Velocidad: se aplica al motor local; emitiendo se conserva
         * y se aplica al volver a local.
         */
        fun setRate(rate: Float) {
            val value = rate.coerceIn(1f, 2f)
            lastRate = value
            if (!((_state.value as? State.Active)?.casting == true)) {
                engine.player.setPlaybackSpeed(value)
            }
            (_state.value as? State.Active)?.let { _state.value = it.copy(rate = value) }
        }

        /** Posición actual validada (0 si no hay nada cargado). */
        fun positionMs(): Long =
            if (isEpisodeActive()) {
                knownPosition()
            } else {
                0
            }

        /**
         * Corte externo (la radio toma el reproductor): finaliza el
         * seguimiento y devuelve la última posición sin tocar el motor.
         */
        fun stopExternal(): Long = finish(positionMs())

        /** Parada del usuario: detiene el motor activo y finaliza. */
        fun stop(): Long {
            val position = positionMs()
            activePlayer().stop()
            return finish(position)
        }

        private fun finish(position: Long): Long {
            finalized = true
            userPaused = true
            pendingReload = false
            cancelRecovery()
            stopTicker()
            detach()
            _state.value = State.Idle
            lastSource = null
            lastEpisode = null
            lastPodcast = null
            voiceBoost.setEpisodeActive(false)
            return position
        }

        private fun activePlayer(): Player = cast.activePlayer

        private fun attach(player: Player) {
            if (listenedPlayer === player) return
            detach()
            player.addListener(listener)
            listenedPlayer = player
        }

        private fun detach() {
            listenedPlayer?.removeListener(listener)
            listenedPlayer = null
        }

        private fun reattach() {
            if (_state.value is State.Idle) return
            if (cast.isCastConnected) attach(activePlayer()) else attach(engine.player)
        }

        private fun markPaused() {
            when (val current = _state.value) {
                is State.WaitingForNetwork ->
                    _state.value =
                        stateFrom(current.episode, current.podcast, current.positionMs, playing = false)
                is State.Reconnecting ->
                    _state.value =
                        stateFrom(current.episode, current.podcast, current.positionMs, playing = false)
                is State.Active ->
                    _state.value = current.copy(playing = false, positionMs = knownPosition())
                else -> Unit
            }
        }

        private fun applyRate(
            rate: Float,
            casting: Boolean,
        ) {
            if (!casting) engine.player.setPlaybackSpeed(rate.coerceIn(1f, 2f))
        }

        @OptIn(UnstableApi::class)
        private fun resumeLocally(current: State.Active) {
            val source = lastSource ?: return
            detach()
            loadLocally(source, current.episode, current.podcast, current.positionMs)
            engine.player.setPlaybackSpeed(current.rate)
            engine.player.playWhenReady = current.playing
            attach(engine.player)
            voiceBoost.setCastConnected(false)
            _state.value = current.copy(casting = false)
        }

        private fun onEnded() {
            if (recoveryJob?.isActive == true) return
            val current = (_state.value as? State.Active) ?: return
            if (finalized) return
            finalized = true
            pendingReload = false
            cancelRecovery()
            stopTicker()
            val durationSec = (durationOf() / MS_PER_SEC).coerceAtLeast(0)
            _events.tryEmit(EpisodePlayerEvent.Ended(current.episode.id, durationSec))
            detach()
            _state.value = State.Idle
            lastSource = null
            lastEpisode = null
            lastPodcast = null
            voiceBoost.setEpisodeActive(false)
        }

        private fun durationOf(): Long {
            val duration = activePlayer().duration
            return duration.coerceAtLeast(0).takeIf { it != Long.MAX_VALUE } ?: 0
        }

        private fun syncPosition() {
            (_state.value as? State.Active)?.let {
                val position = knownPosition()
                lastPositionMs = position
                _state.value = it.copy(positionMs = position, durationMs = durationOf())
            }
        }

        private fun startTicker() {
            pollJob?.cancel()
            pollJob =
                // Main obligatorio: getCurrentPosition/getDuration exigen el hilo de creación.
                scope.launch(Dispatchers.Main.immediate) {
                    while (true) {
                        delay(POSITION_POLL_MS)
                        if (_state.value !is State.Active) return@launch
                        syncPosition()
                    }
                }
        }

        private fun stopTicker() {
            pollJob?.cancel()
            pollJob = null
        }

        /** Carga en el player local la fuente del episodio desde [positionMs]. */
        @OptIn(UnstableApi::class)
        private fun loadLocally(
            source: EpisodePlaybackSource,
            episode: PodcastEpisode,
            podcast: PodcastSummary?,
            positionMs: Long,
        ) {
            engine.player.setMediaSource(mediaItems.createEpisodeSource(mediaItemFor(source, episode, podcast)))
            engine.player.prepare()
            if (positionMs > 0) engine.player.seekTo(positionMs)
            attach(engine.player)
        }

        private fun mediaItemFor(
            source: EpisodePlaybackSource,
            episode: PodcastEpisode,
            podcast: PodcastSummary?,
        ): MediaItem =
            MediaItem.Builder()
                .setUri(source.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setArtist(podcast?.title)
                        .setArtworkUri(podcast?.imageUrl?.let { android.net.Uri.parse(it) })
                        .build(),
                )
                .build()

        private fun currentPosition(): Long {
            return runCatching { activePlayer().currentPosition }
                .getOrDefault(lastPositionMs)
                .coerceAtLeast(0)
        }

        /** La posición del player puede quedar a 0 tras un error; manda la última conocida. */
        private fun knownPosition(): Long = maxOf(currentPosition(), lastPositionMs)

        /**
         * Un fallo de red se reintenta; formato, decodificación, auth o 4xx no
         * mejoran repitiendo la misma URL.
         */
        private fun shouldRecover(error: PlaybackException): Boolean {
            val source = lastSource ?: return false
            if (source.local || cast.isCastConnected) return false
            val http = PlaybackErrorClassifier.httpStatus(error)
            if (http != null && http in HTTP_CLIENT_ERROR_MIN..HTTP_CLIENT_ERROR_MAX) return false
            return when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                -> true
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                -> false
                else -> http == null || http >= HTTP_SERVER_ERROR_MIN
            }
        }

        /**
         * Espera a la red sin consumir intentos y, ya online, reintenta con
         * backoff acotado. Al agotar el presupuesto deja [State.Error] y la
         * posición para que el botón play reintente a mano.
         */
        @Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
        private fun startRecovery(position: Long) {
            if (recoveryJob?.isActive == true) return
            val episode = lastEpisode ?: return
            val podcast = lastPodcast
            pendingReload = true
            stopTicker()
            recoveryJob =
                scope.launch(Dispatchers.Main.immediate) {
                    var attempt = 0
                    var startedAt = clock.nowMs()
                    while (isActive && attempt < policy.maxAttempts) {
                        if (userPaused || finalized) return@launch
                        if (!connectivity.isOnline.value) {
                            _state.value = State.WaitingForNetwork(episode, podcast, position)
                            connectivity.awaitOnline()
                            if (userPaused || finalized) return@launch
                            startedAt = clock.nowMs()
                        }
                        if (clock.nowMs() - startedAt >= policy.totalTimeoutMs) break
                        attempt++
                        _state.value =
                            State.Reconnecting(
                                episode = episode,
                                podcast = podcast,
                                positionMs = position,
                                attempt = attempt,
                                maxAttempts = policy.maxAttempts,
                            )
                        policy.delayForAttempt(attempt).takeIf { it > 0 }?.let { delay(it) }
                        if (userPaused || finalized) return@launch
                        if (!connectivity.isOnline.value) continue
                        val source = lastSource ?: break
                        loadLocally(source, episode, podcast, position)
                        engine.player.playWhenReady = true
                        attach(engine.player)
                        if (awaitReady(policy.attemptTimeoutMs)) {
                            recovered(episode, podcast, position)
                            return@launch
                        }
                    }
                    if (!finalized && !userPaused) {
                        fail(episode, position, "No se pudo reanudar el episodio. Comprueba tu conexión.")
                    }
                }
        }

        private suspend fun awaitReady(timeoutMs: Long): Boolean {
            val deadline = clock.nowMs() + timeoutMs
            while (clock.nowMs() < deadline) {
                if (userPaused || finalized) return false
                val player = engine.player
                if (player.isPlaying || player.playbackState == Player.STATE_READY) return true
                delay(READY_POLL_MS)
            }
            return false
        }

        private fun recovered(
            episode: PodcastEpisode,
            podcast: PodcastSummary?,
            position: Long,
        ) {
            pendingReload = false
            _state.value =
                stateFrom(episode, podcast, position, playing = true)
            startTicker()
        }

        private fun stateFrom(
            episode: PodcastEpisode,
            podcast: PodcastSummary?,
            position: Long,
            playing: Boolean,
        ): State.Active =
            State.Active(
                episode = episode,
                podcast = podcast,
                playing = playing,
                positionMs = position,
                durationMs = durationOf(),
                rate = lastRate,
                local = lastSource?.local == true,
                casting = false,
            )

        private fun fail(
            episode: PodcastEpisode,
            position: Long,
            message: String,
        ) {
            pendingReload = true
            cancelRecovery()
            stopTicker()
            detach()
            _state.value = State.Error(episode, lastPodcast, position, lastRate, message)
            _events.tryEmit(EpisodePlayerEvent.Failed(episode.id, message))
            voiceBoost.setEpisodeActive(false)
        }

        private fun cancelRecovery() {
            recoveryJob?.cancel()
            recoveryJob = null
        }

        private companion object {
            const val HTTP_CLIENT_ERROR_MIN = 400
            const val HTTP_CLIENT_ERROR_MAX = 499
            const val HTTP_SERVER_ERROR_MIN = 500
        }
    }
