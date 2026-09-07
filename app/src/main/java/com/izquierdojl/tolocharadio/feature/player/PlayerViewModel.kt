package com.izquierdojl.tolocharadio.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.api.streamUrl
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.PlaybackRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado sellado del player (constitución IV). */
sealed interface PlayerState {
    data object Idle : PlayerState

    data class Buffering(val station: StationDto) : PlayerState

    data class Playing(val station: StationDto) : PlayerState

    data class Paused(val station: StationDto) : PlayerState

    data class Error(val station: StationDto?, val message: String) : PlayerState

    /** Extrae la estación si el estado la contiene, o null en Idle/Error sin estación. */
    fun stationOrNull(): StationDto? =
        when (this) {
            is Buffering -> station
            is Playing -> station
            is Paused -> station
            is Error -> station
            Idle -> null
        }
}

/**
 * Mini-player persistente: precheck `status`, proxy con Bearer,
 * reintento con backoff y supervivencia a navegación/rotación (US-5).
 */
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playback: PlaybackRepo,
        private val prefs: InstancePrefs,
        private val dataSource: AuthDataSourceFactory,
        private val activeStationHolder: ActiveStationHolder,
        val exoPlayer: ExoPlayer,
    ) : ViewModel() {
        private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
        val state: StateFlow<PlayerState> = _state.asStateFlow()

        /**
         * Silencio local (spec 004, FR-005): modificador independiente que
         * corta el volumen sin detener la emisión. Se resetea en [play]
         * y [stop] (acuerdo de clarify: siempre vuelve con sonido).
         */
        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private var retryJob: Job? = null
        private var loadJob: Job? = null
        private var controller: MediaController? = null

        private val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val current =
                        (_state.value as? PlayerState.Playing)?.station
                            ?: (_state.value as? PlayerState.Buffering)?.station
                            ?: (_state.value as? PlayerState.Paused)?.station
                            ?: return
                    _state.value =
                        when (playbackState) {
                            Player.STATE_READY ->
                                if (exoPlayer.playWhenReady) {
                                    PlayerState.Playing(current)
                                } else {
                                    PlayerState.Paused(current)
                                }
                            Player.STATE_BUFFERING -> PlayerState.Buffering(current)
                            Player.STATE_ENDED, Player.STATE_IDLE -> PlayerState.Idle
                            else -> _state.value
                        }
                    syncHolderToState()
                }

                override fun onPlayerError(error: PlaybackException) {
                    val current = (_state.value as? PlayerState.Playing)?.station
                    _state.value = PlayerState.Error(current, "Se ha interrumpido la reproducción.")
                    syncHolderToState()
                    scheduleRetry(current, attempt = 1)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    val current =
                        (_state.value as? PlayerState.Playing)?.station
                            ?: (_state.value as? PlayerState.Paused)?.station
                            ?: (_state.value as? PlayerState.Buffering)?.station
                            ?: return
                    _state.value = if (isPlaying) PlayerState.Playing(current) else PlayerState.Paused(current)
                    syncHolderToState()
                }
            }

        init {
            exoPlayer.addListener(listener)
            connectController()
            syncFromHolder()
        }

        /**
         * Sincroniza el estado inicial desde [activeStationHolder] al recrearse
         * el ViewModel (spec 010, US3 - persistencia del mini-player).
         * Si el ExoPlayer sigue reproduciendo/pausado, reconstruye el PlayerState.
         */
        private fun syncFromHolder() {
            val station = activeStationHolder.station ?: return
            val holderState = activeStationHolder.playerState
            _state.value =
                when (holderState) {
                    PlayerStateType.PLAYING -> PlayerState.Playing(station)
                    PlayerStateType.PAUSED -> PlayerState.Paused(station)
                    PlayerStateType.BUFFERING -> PlayerState.Buffering(station)
                    PlayerStateType.ERROR -> PlayerState.Error(station, "Se ha interrumpido la reproducción.")
                    PlayerStateType.IDLE -> PlayerState.Idle
                }
        }

        /**
         * Conexión best-effort al controlador de sesión (notificación /
         * controles externos). El estado lo gobierna el listener del
         * `ExoPlayer` compartido, así que un fallo aquí nunca debe impedir
         * que el VM nazca en `Idle`.
         */
        @OptIn(UnstableApi::class)
        private fun connectController() {
            runCatching {
                val token = SessionToken(context, ComponentName(context, RadioPlaybackService::class.java))
                val future = MediaController.Builder(context, token).buildAsync()
                future.addListener(
                    { controller = runCatching { future.get() }.getOrNull() },
                    MoreExecutors.directExecutor(),
                )
            }
        }

        /** Reproduce tras el precheck `playable` (FR-007). Resetea el silencio. */
        fun play(station: StationDto) {
            retryJob?.cancel()
            loadJob?.cancel()
            _isMuted.value = false
            exoPlayer.volume = 1f
            _state.value = PlayerState.Buffering(station)
            syncHolderToState()
            loadJob =
                viewModelScope.launch {
                    when (val r = playback.status(station.id)) {
                        is ApiResult.Ok -> {
                            if (!r.value.playable) {
                                _state.value =
                                    PlayerState.Error(
                                        station,
                                        r.value.reason?.ifBlank { null } ?: "Emisora no disponible.",
                                    )
                                syncHolderToState()
                                return@launch
                            }
                            startStream(station)
                        }
                        is ApiResult.Err -> {
                            _state.value = PlayerState.Error(station, r.error.userMessage())
                            syncHolderToState()
                        }
                    }
                }
        }

        @OptIn(UnstableApi::class)
        private suspend fun startStream(station: StationDto) {
            val base = prefs.baseUrl.first()
            val metadata =
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist("Tolocha Radio")
                    .setArtworkUri(station.favicon?.let { android.net.Uri.parse(it) })
                    .setAlbumTitle("Tolocha Radio")
                    .build()
            val item =
                MediaItem.Builder()
                    .setUri(streamUrl(base, station.id))
                    .setMediaMetadata(metadata)
                    .build()
            exoPlayer.setMediaSource(
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSource)
                    .createMediaSource(item),
            )
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            _state.value = PlayerState.Buffering(station)
            syncHolderToState()
        }

        /** Silencia o restaura el sonido sin detener la emisión (spec 004, FR-005). */
        fun toggleMute() {
            _isMuted.value = !_isMuted.value
            exoPlayer.volume = if (_isMuted.value) 0f else 1f
        }

        /**
         * Sincroniza el estado actual del [_state] hacia [activeStationHolder]
         * para persistirlo entre recreaciones de Activity/ViewModel.
         */
        private fun syncHolderToState() {
            val current = _state.value
            val holderState =
                when (current) {
                    is PlayerState.Playing -> PlayerStateType.PLAYING
                    is PlayerState.Paused -> PlayerStateType.PAUSED
                    is PlayerState.Buffering -> PlayerStateType.BUFFERING
                    is PlayerState.Error -> PlayerStateType.ERROR
                    PlayerState.Idle -> PlayerStateType.IDLE
                }
            activeStationHolder.update(current.stationOrNull(), holderState)
        }

        /** Cancela un intento de carga en curso y vuelve a `Idle` (spec 004, FR-004). */
        fun cancelLoad() {
            loadJob?.cancel()
            retryJob?.cancel()
            if (_state.value is PlayerState.Buffering) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                _state.value = PlayerState.Idle
                activeStationHolder.clear()
            }
        }

        /** Pausa o reanuda según el estado actual. */
        fun toggle() {
            when (_state.value) {
                is PlayerState.Playing -> exoPlayer.playWhenReady = false
                is PlayerState.Paused -> exoPlayer.playWhenReady = true
                else -> Unit
            }
            syncHolderToState()
        }

        /** Detiene y vuelve a Idle. Resetea el silencio. */
        fun stop() {
            retryJob?.cancel()
            loadJob?.cancel()
            _isMuted.value = false
            exoPlayer.volume = 1f
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _state.value = PlayerState.Idle
            activeStationHolder.clear()
        }

        /** Reintento manual desde el estado Error. */
        fun retry() {
            val station = (_state.value as? PlayerState.Error)?.station ?: return
            play(station)
        }

        private fun scheduleRetry(
            station: StationDto?,
            attempt: Int,
        ) {
            if (station == null || attempt > MAX_RETRY) return
            retryJob?.cancel()
            retryJob =
                viewModelScope.launch {
                    delay(RETRY_BASE_MS * attempt)
                    if (_state.value is PlayerState.Error) play(station)
                }
        }

        override fun onCleared() {
            retryJob?.cancel()
            loadJob?.cancel()
            exoPlayer.removeListener(listener)
            controller?.release()
            super.onCleared()
        }

        private companion object {
            const val MAX_RETRY = 3
            const val RETRY_BASE_MS = 2_000L
        }
    }
