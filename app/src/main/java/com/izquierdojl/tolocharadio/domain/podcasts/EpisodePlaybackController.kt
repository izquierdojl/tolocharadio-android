package com.izquierdojl.tolocharadio.domain.podcasts

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.di.ApplicationScope
import com.izquierdojl.tolocharadio.feature.player.PlaybackEngine
import com.izquierdojl.tolocharadio.feature.player.StationMediaItemFactory
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Intervalo de muestreo de posición del episodio en curso (ms). */
private const val POSITION_POLL_MS = 1_000L

/** Milisegundos por segundo. */
private const val MS_PER_SEC = 1_000L

/** Eventos del controlador VOD hacia el ViewModel. */
sealed interface EpisodePlayerEvent {
    data class Ended(val episodeId: Long, val durationSec: Long) : EpisodePlayerEvent

    data class Failed(val episodeId: Long, val message: String) : EpisodePlayerEvent
}

/**
 * Pilota el reproductor para episodios (research D10): fichero finito
 * con URL firmada o archivo local, sin reconexión live.
 *
 * Emite al receptor cuando hay sesión Cast (misma política que la
 * radio, T042); si se pierde la conexión, reanuda en local en la
 * última posición conocida. La radio y los episodios nunca suenan a
 * la vez: quien arranca corta al otro (PlayerViewModel ↔ [stopExternal]).
 */
@Singleton
@Suppress("TooManyFunctions")
class EpisodePlaybackController
    @Inject
    constructor(
        private val engine: PlaybackEngine,
        private val cast: CastPlayerManager,
        private val mediaItems: StationMediaItemFactory,
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

            data class Error(val episode: PodcastEpisode, val message: String) : State
        }

        private val _state = MutableStateFlow<State>(State.Idle)
        val state: StateFlow<State> = _state.asStateFlow()

        private val _events = MutableSharedFlow<EpisodePlayerEvent>(extraBufferCapacity = 1)
        val events: SharedFlow<EpisodePlayerEvent> = _events.asSharedFlow()

        private var pollJob: Job? = null
        private var finalized = false
        private var listenedPlayer: Player? = null
        private var lastSource: EpisodePlaybackSource? = null

        /** `true` cuando hay un episodio cargado (la radio debe ignorar eventos). */
        fun isEpisodeActive(): Boolean = _state.value !is State.Idle

        private val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) onEnded()
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val current = (_state.value as? State.Active)?.episode ?: return
                    // Sin PII: solo código y tipo de causa para diagnóstico.
                    Log.e(
                        "EpisodePlayback",
                        "code=${error.errorCode} cause=${error.cause?.javaClass?.simpleName}",
                    )
                    _state.value = State.Error(current, "No se pudo reproducir el episodio.")
                    _events.tryEmit(EpisodePlayerEvent.Failed(current.id, ""))
                    stopTicker()
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
            stopTicker()
            lastSource = source
            _state.value = State.Loading(episode)
            val casting = cast.isCastConnected
            if (casting) {
                cast.connectToEpisode(source.uri, episode.title, podcast?.title)
                attach(activePlayer())
            } else {
                val item =
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
                engine.player.setMediaSource(mediaItems.createEpisodeSource(item))
                engine.player.prepare()
                if (startMs > 0) engine.player.seekTo(startMs)
                attach(engine.player)
            }
            applyRate(rate, casting)
            activePlayer().playWhenReady = true
            _state.value =
                State.Active(episode, podcast, playing = true, startMs, durationOf(), rate, source.local, casting)
            startTicker()
        }

        fun pause() {
            activePlayer().playWhenReady = false
            (_state.value as? State.Active)?.let { _state.value = it.copy(playing = false) }
        }

        fun resume() {
            activePlayer().playWhenReady = true
            (_state.value as? State.Active)?.let { _state.value = it.copy(playing = true) }
        }

        fun seekTo(positionMs: Long) {
            activePlayer().seekTo(positionMs.coerceAtLeast(0))
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
            if (!((_state.value as? State.Active)?.casting == true)) {
                engine.player.setPlaybackSpeed(value)
            }
            (_state.value as? State.Active)?.let { _state.value = it.copy(rate = value) }
        }

        /** Posición actual validada (0 si no hay nada cargado). */
        fun positionMs(): Long =
            if (isEpisodeActive()) {
                activePlayer().currentPosition.coerceAtLeast(0)
            } else {
                0
            }

        /**
         * Corte externo (la radio toma el reproductor): finaliza el
         * seguimiento y devuelve la última posición sin tocar el motor.
         */
        fun stopExternal(): Long {
            val position = positionMs()
            finalized = true
            stopTicker()
            detach()
            _state.value = State.Idle
            lastSource = null
            return position
        }

        /** Parada del usuario: detiene el motor activo y finaliza. */
        fun stop(): Long {
            activePlayer().stop()
            return stopExternal()
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
            val item =
                MediaItem.Builder()
                    .setUri(source.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(current.episode.title)
                            .setArtist(current.podcast?.title)
                            .build(),
                    )
                    .build()
            engine.player.setMediaSource(mediaItems.createEpisodeSource(item))
            engine.player.prepare()
            engine.player.seekTo(current.positionMs)
            engine.player.setPlaybackSpeed(current.rate)
            engine.player.playWhenReady = current.playing
            attach(engine.player)
            _state.value = current.copy(casting = false)
        }

        private fun onEnded() {
            val current = (_state.value as? State.Active) ?: return
            if (finalized) return
            finalized = true
            stopTicker()
            val durationSec = (durationOf() / MS_PER_SEC).coerceAtLeast(0)
            _events.tryEmit(EpisodePlayerEvent.Ended(current.episode.id, durationSec))
            detach()
            _state.value = State.Idle
            lastSource = null
        }

        private fun durationOf(): Long {
            val duration = activePlayer().duration
            return duration.coerceAtLeast(0).takeIf { it != Long.MAX_VALUE } ?: 0
        }

        private fun syncPosition() {
            (_state.value as? State.Active)?.let {
                _state.value = it.copy(positionMs = positionMs(), durationMs = durationOf())
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
    }
