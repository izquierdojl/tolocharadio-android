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
import com.izquierdojl.tolocharadio.cast.CastConnectionState
import com.izquierdojl.tolocharadio.cast.CastNotice
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.cast.CastPlayerState
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.data.repo.PlaybackRepo
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import com.izquierdojl.tolocharadio.domain.playback.PlaybackStatusReason
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
 * Mini-player persistente: preestado de disponibilidad (precheck) bloqueante y
 * reproducción siempre por el proxy autenticado (spec 0021: las listas y el HLS
 * los resuelve el servicio), con supervivencia a navegación/rotación (US-5).
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
        private val history: HistoryRepo,
        private val prefs: InstancePrefs,
        private val activeStationHolder: ActiveStationHolder,
        val exoPlayer: ExoPlayer,
        val castPlayerManager: CastPlayerManager,
        private val volumeController: PlaybackVolumeController,
        private val resolveSource: ResolvePlaybackSourceUseCase,
        private val mediaItemFactory: StationMediaItemFactory,
    ) : ViewModel() {
        private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
        val state: StateFlow<PlayerState> = _state.asStateFlow()

        /**
         * Silencio de la salida activa (spec 004/0035, FR-007/FR-015): fuente única en
         * [PlaybackVolumeController] (local o Cast). Se resetea en [play] y [stop]
         * (acuerdo de clarify: siempre vuelve con sonido).
         */
        val isMuted: StateFlow<Boolean> = volumeController.muted

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

        /** Emisora y dispositivo pendientes de reenviar al reconectar (bug 0032). */
        private var lostStation: StationDto? = null
        private var lostDeviceName: String? = null

        private val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    // Con Cast conectado el estado lo gobierna el CastPlayer.
                    if (castPlayerManager.isCastConnected) return
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
                    if (castPlayerManager.isCastConnected) return
                    val current = activeStation() ?: return
                    setError(current, "Se ha interrumpido la reproducción.")
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (castPlayerManager.isCastConnected) return
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
            observeCastNotices()
            observeCastReconnection()
        }

        /**
         * Avisos de sesión Cast (bug 0031): si la sesión se pierde, la app no
         * arranca audio local (el receptor puede seguir sonando) y muestra un
         * aviso accionable; el usuario decide reanudar en local con `retry()`.
         */
        private fun observeCastNotices() {
            viewModelScope.launch {
                castPlayerManager.notices.collect { notice ->
                    when (notice) {
                        is CastNotice.SessionLost -> showCastSessionLost(notice.deviceName)
                    }
                }
            }
        }

        /**
         * Reenvío automático al reconectar (bug 0032): si la sesión se había
         * perdido, en cuanto el **mismo** dispositivo vuelve a conectarse se
         * reenvía la emisora sin pasos manuales. Nunca se envía a otro
         * dispositivo distinto al que se perdió.
         */
        private fun observeCastReconnection() {
            viewModelScope.launch {
                castPlayerManager.connectionState.collect { state ->
                    val station = lostStation ?: return@collect
                    if (state != CastConnectionState.CONNECTED) return@collect
                    val expectedDevice = lostDeviceName
                    val connectedDevice =
                        (castPlayerManager.castState.value as? CastPlayerState.Cast)?.deviceName
                    lostStation = null
                    lostDeviceName = null
                    if (expectedDevice == null || connectedDevice == null || connectedDevice == expectedDevice) {
                        play(station)
                    }
                }
            }
        }

        private fun showCastSessionLost(deviceName: String) {
            val station = _state.value.stationOrNull() ?: activeStationHolder.station ?: return
            lostStation = station
            lostDeviceName = deviceName
            _state.value =
                PlayerState.Error(
                    station,
                    "Se perdió la conexión con $deviceName. Puede seguir sonando allí; " +
                        "reintenta para escuchar en el móvil.",
                )
            syncHolderToState()
            syncCastState()
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
         * Reproduce [station]: consulta el preestado de disponibilidad
         * (bloqueante para todo tipo de emisora) y arranca la reproducción por
         * el proxy autenticado si está disponible.
         */
        fun play(station: StationDto) {
            loadJob?.cancel()
            lostStation = null
            lostDeviceName = null
            resetPlaybackSession()
            _state.value = PlayerState.Buffering(station)
            syncHolderToState()
            syncCastState()
            loadJob =
                viewModelScope.launch {
                    when (val r = playback.status(station.id)) {
                        is ApiResult.Ok -> {
                            if (!r.value.playable) {
                                setError(station, PlaybackStatusReason.reasonToMessage(r.value.reason))
                            } else {
                                startSource(station, resolveSource(station))
                            }
                        }
                        is ApiResult.Err -> setError(station, r.error.userMessage())
                    }
                }
        }

        @OptIn(UnstableApi::class)
        private suspend fun startSource(
            station: StationDto,
            source: PlaybackSource,
        ) {
            val base = prefs.baseUrl.first()
            if (castPlayerManager.isCastConnected) {
                // Cast reproduce por la URL pública (0026): el servidor no
                // registra la escucha, así que no se inserta en el historial.
                castPlayerManager.connectToStation(station, source)
            } else {
                val item = mediaItemFactory.create(station, source, base)
                castPlayerManager.exoPlayer.setMediaSource(mediaItemFactory.createMediaSource(item, source))
                castPlayerManager.exoPlayer.prepare()
                castPlayerManager.exoPlayer.playWhenReady = true
                // El proxy registrará la escucha server-side; localmente se
                // refleja al momento para que Historial/accesos se actualicen.
                history.recordLocalPlay(station)
            }
            syncCastState()
        }

        /** Silencia o restaura la salida activa sin detener la emisión (FR-007). */
        fun toggleMute() {
            volumeController.toggleMute()
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

        /** Estado efectivo: el de Cast si hay sesión activa, si no el local. */
        private fun effectiveState(): PlayerState =
            (castPlayerManager.castState.value as? CastPlayerState.Cast)?.playerState ?: _state.value

        private fun setError(
            station: StationDto?,
            message: String,
        ) {
            _state.value = PlayerState.Error(station, message)
            syncHolderToState()
            syncCastState()
        }

        private fun resetPlaybackSession() {
            volumeController.resetMute()
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

        /** Refleja el estado local en el estado de Cast (solo sin conexión Cast). */
        private fun syncCastState() {
            if (!castPlayerManager.isCastConnected) {
                castPlayerManager.updateLocalState(_state.value)
            }
        }

        /** Cancela un intento de carga en curso y vuelve a `Idle` (spec 004, FR-004). */
        fun cancelLoad() {
            loadJob?.cancel()
            if (effectiveState() is PlayerState.Buffering) {
                castPlayerManager.activePlayer.stop()
                castPlayerManager.activePlayer.clearMediaItems()
                _state.value = PlayerState.Idle
                activeStationHolder.clear()
                syncCastState()
            }
        }

        /** Pausa o reanuda según el estado actual (local o Cast). */
        fun toggle() {
            val player = castPlayerManager.activePlayer
            when (effectiveState()) {
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
            lostStation = null
            lostDeviceName = null
            volumeController.resetMute()
            _fullPlayerVisible.value = false
            castPlayerManager.activePlayer.stop()
            castPlayerManager.activePlayer.clearMediaItems()
            _state.value = PlayerState.Idle
            activeStationHolder.clear()
            syncCastState()
        }

        /** Reintento manual desde el estado Error (local o Cast). */
        fun retry() {
            val station =
                (effectiveState() as? PlayerState.Error)?.station
                    ?: (_state.value as? PlayerState.Error)?.station
                    ?: return
            play(station)
        }

        override fun onCleared() {
            loadJob?.cancel()
            castPlayerManager.exoPlayer.removeListener(listener)
            castPlayerManager.release()
            controller?.release()
            super.onCleared()
        }
    }
