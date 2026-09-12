package com.izquierdojl.tolocharadio.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.cast.CastPlayerState
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.PlaybackRepo
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import com.izquierdojl.tolocharadio.domain.playback.ResolutionResult
import com.izquierdojl.tolocharadio.domain.playback.ResolvePlaybackSourceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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
 * Mini-player persistente: precheck `status`, proxy con Bearer o fuentes
 * directas resueltas (HLS/listas, spec 0019), reintento acotado de candidatos
 * y supervivencia a navegación/rotación (US-5).
 *
 * `TooManyFunctions`/`LongParameterList` suprimidos: el player es un estado
 * central (spec 004/0018/0019) que agrupa reproducción, silencio, Cast y full
 * player; fragmentarlo añadiría indirección sin valor (constitución V, YAGNI).
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playback: PlaybackRepo,
        private val prefs: InstancePrefs,
        private val activeStationHolder: ActiveStationHolder,
        val exoPlayer: ExoPlayer,
        val castPlayerManager: CastPlayerManager,
        private val resolveSource: ResolvePlaybackSourceUseCase,
        private val mediaItemFactory: StationMediaItemFactory,
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

        /**
         * Reproductor a pantalla completa visible (spec 0018, FR-005). Se
         * abre al pulsar un acceso directo del icono y se cierra al detener.
         */
        private val _fullPlayerVisible = MutableStateFlow(false)
        val fullPlayerVisible: StateFlow<Boolean> = _fullPlayerVisible.asStateFlow()

        /** Estado de Cast para la UI (FR-006, FR-011). */
        val castState: StateFlow<CastPlayerState> = castPlayerManager.castState

        private var loadJob: Job? = null
        private var controller: MediaController? = null

        /** Cola de candidatos activa (solo emisoras de lista) y saltos consumidos. */
        private var playlistQueue: PlaylistPlaybackQueue? = null
        private var playlistAttempts = 0

        private val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val current = activeStation() ?: return
                    _state.value =
                        when (playbackState) {
                            Player.STATE_READY ->
                                if (castPlayerManager.activePlayer.playWhenReady) {
                                    PlayerState.Playing(current)
                                } else {
                                    PlayerState.Paused(current)
                                }
                            Player.STATE_BUFFERING -> PlayerState.Buffering(current)
                            Player.STATE_ENDED, Player.STATE_IDLE -> PlayerState.Idle
                            else -> _state.value
                        }
                    syncHolderToState()
                    syncCastState()
                }

                override fun onPlayerError(error: PlaybackException) {
                    val current = activeStation() ?: return
                    val queue = playlistQueue
                    if (queue != null && playlistAttempts < MAX_PLAYLIST_ATTEMPTS && !queue.exhausted) {
                        queue.advance()
                        playlistAttempts++
                        loadJob = viewModelScope.launch { startSource(current, queue.current) }
                        return
                    }
                    setError(current, "Se ha interrumpido la reproducción.")
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    val current = activeStation() ?: return
                    _state.value = if (isPlaying) PlayerState.Playing(current) else PlayerState.Paused(current)
                    syncHolderToState()
                    syncCastState()
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
            syncCastState()
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

        /**
         * Reproduce [station]: resuelve la fuente (proxy, HLS o lista) y arranca
         * la reproducción. Resetea silencio y estado de fallback.
         */
        fun play(station: StationDto) {
            loadJob?.cancel()
            resetPlaybackSession()
            _state.value = PlayerState.Buffering(station)
            syncHolderToState()
            syncCastState()
            loadJob =
                viewModelScope.launch {
                    when (val result = resolveSource(station)) {
                        is ResolutionResult.Proxied -> playProxied(station)
                        is ResolutionResult.Single -> startSource(station, result.source)
                        is ResolutionResult.Candidates -> {
                            playlistQueue = PlaylistPlaybackQueue(result.sources)
                            playlistAttempts = 1
                            startSource(station, result.sources.first())
                        }
                        is ResolutionResult.Unavailable -> setError(station, result.error.userMessage())
                    }
                }
        }

        /** Emisora directa: mantiene el precheck `playable` bloqueante (FR-007). */
        private suspend fun playProxied(station: StationDto) {
            when (val r = playback.status(station.id)) {
                is ApiResult.Ok -> {
                    if (!r.value.playable) {
                        setError(station, r.value.reason?.ifBlank { null } ?: "Emisora no disponible.")
                    } else {
                        startSource(station, PlaybackSource.Proxied(station.id))
                    }
                }
                is ApiResult.Err -> setError(station, r.error.userMessage())
            }
        }

        @OptIn(UnstableApi::class)
        private suspend fun startSource(
            station: StationDto,
            source: PlaybackSource,
        ) {
            val base = prefs.baseUrl.first()
            activeStationHolder.updateResolvedSource(source)
            if (castPlayerManager.isCastConnected) {
                castPlayerManager.connectToStation(station, source)
            } else {
                val item = mediaItemFactory.create(station, source, base)
                castPlayerManager.exoPlayer.setMediaSource(mediaItemFactory.createMediaSource(item, source))
                castPlayerManager.exoPlayer.prepare()
                castPlayerManager.exoPlayer.playWhenReady = true
            }
            _state.value = PlayerState.Buffering(station)
            syncHolderToState()
            syncCastState()
        }

        /** Silencia o restaura el sonido sin detener la emisión (spec 004, FR-005). */
        fun toggleMute() {
            _isMuted.value = !_isMuted.value
            castPlayerManager.exoPlayer.volume = if (_isMuted.value) 0f else 1f
        }

        /** Abre el reproductor a pantalla completa (spec 0018, FR-005). */
        fun openFullPlayer() {
            _fullPlayerVisible.value = true
        }

        /** Cierra el reproductor a pantalla completa (spec 0018, FR-005). */
        fun closeFullPlayer() {
            _fullPlayerVisible.value = false
        }

        private fun activeStation(): StationDto? =
            (_state.value as? PlayerState.Playing)?.station
                ?: (_state.value as? PlayerState.Buffering)?.station
                ?: (_state.value as? PlayerState.Paused)?.station
                ?: (_state.value as? PlayerState.Error)?.station

        private fun setError(
            station: StationDto?,
            message: String,
        ) {
            _state.value = PlayerState.Error(station, message)
            syncHolderToState()
            syncCastState()
        }

        private fun resetPlaybackSession() {
            _isMuted.value = false
            castPlayerManager.exoPlayer.volume = 1f
            playlistQueue = null
            playlistAttempts = 0
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

        /** Sincroniza el estado de Cast para la UI. */
        private fun syncCastState() {
            castPlayerManager.updateCastPlayerState(_state.value)
        }

        /** Cancela un intento de carga en curso y vuelve a `Idle` (spec 004, FR-004). */
        fun cancelLoad() {
            loadJob?.cancel()
            if (_state.value is PlayerState.Buffering) {
                castPlayerManager.exoPlayer.stop()
                castPlayerManager.exoPlayer.clearMediaItems()
                _state.value = PlayerState.Idle
                activeStationHolder.clear()
                playlistQueue = null
                playlistAttempts = 0
                syncCastState()
            }
        }

        /** Pausa o reanuda según el estado actual. */
        fun toggle() {
            val player = castPlayerManager.activePlayer
            when (_state.value) {
                is PlayerState.Playing -> player.playWhenReady = false
                is PlayerState.Paused -> player.playWhenReady = true
                else -> Unit
            }
            syncHolderToState()
            syncCastState()
        }

        /** Detiene y vuelve a Idle. Resetea el silencio y el fallback. */
        fun stop() {
            loadJob?.cancel()
            _isMuted.value = false
            _fullPlayerVisible.value = false
            castPlayerManager.exoPlayer.volume = 1f
            castPlayerManager.activePlayer.stop()
            castPlayerManager.activePlayer.clearMediaItems()
            _state.value = PlayerState.Idle
            activeStationHolder.clear()
            playlistQueue = null
            playlistAttempts = 0
            syncCastState()
        }

        /** Reintento manual desde el estado Error (reinicia la resolución). */
        fun retry() {
            val station = (_state.value as? PlayerState.Error)?.station ?: return
            play(station)
        }

        override fun onCleared() {
            loadJob?.cancel()
            castPlayerManager.exoPlayer.removeListener(listener)
            castPlayerManager.release()
            controller?.release()
            super.onCleared()
        }

        private companion object {
            const val MAX_PLAYLIST_ATTEMPTS = 3
        }
    }
