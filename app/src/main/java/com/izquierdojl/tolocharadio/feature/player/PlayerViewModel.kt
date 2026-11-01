package com.izquierdojl.tolocharadio.feature.player

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.izquierdojl.tolocharadio.cast.CastConnectionState
import com.izquierdojl.tolocharadio.cast.CastNotice
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.cast.CastPlayerState
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.PlaybackRepo
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import com.izquierdojl.tolocharadio.domain.playback.PlaybackStatusReason
import com.izquierdojl.tolocharadio.domain.playback.PlayerErrorAction
import com.izquierdojl.tolocharadio.domain.playback.RecoveryPolicy
import com.izquierdojl.tolocharadio.domain.playback.RecoveryState
import com.izquierdojl.tolocharadio.domain.playback.ResolvePlaybackSourceUseCase
import com.izquierdojl.tolocharadio.domain.playback.StationAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Estado sellado del player (constitución IV). */
sealed interface PlayerState {
    data object Idle : PlayerState

    data class Buffering(
        val station: StationDto,
        val retrying: Boolean = false,
        val waitingForNetwork: Boolean = false,
    ) : PlayerState

    data class Playing(val station: StationDto) : PlayerState

    data class Paused(val station: StationDto) : PlayerState

    data class Reconnecting(
        val station: StationDto,
        val attempt: Int,
        val maxAttempts: Int,
        val waitingForNetwork: Boolean = false,
        val applying: Boolean = false,
    ) : PlayerState

    data class Error(
        val station: StationDto?,
        val message: String,
        val action: PlayerErrorAction = PlayerErrorAction.RETRY,
    ) : PlayerState

    /** Extrae la estación si el estado la contiene, o null en Idle/Error sin estación. */
    fun stationOrNull(): StationDto? =
        when (this) {
            is Buffering -> station
            is Playing -> station
            is Paused -> station
            is Reconnecting -> station
            is Error -> station
            Idle -> null
        }
}

/**
 * Mini-player persistente: preestado de disponibilidad (precheck) bloqueante y
 * reproducción siempre por el proxy autenticado (spec 0021).
 *
 * Desde la spec 0042 el player es [PlaybackEngine]; este ViewModel refleja el
 * estado, delega el arranque local en [LocalPlaybackStarter] y ofrece reintento.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playback: PlaybackRepo,
        private val activeStationHolder: ActiveStationHolder,
        private val engine: PlaybackEngine,
        val castPlayerManager: CastPlayerManager,
        private val volumeController: PlaybackVolumeController,
        private val resolveSource: ResolvePlaybackSourceUseCase,
        private val localPlaybackStarter: LocalPlaybackStarter,
        private val recoveryManager: PlaybackRecoveryManager,
        private val profileController: PlaybackProfileController,
    ) : ViewModel() {
        private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
        val state: StateFlow<PlayerState> = _state.asStateFlow()

        val isMuted: StateFlow<Boolean> = volumeController.muted

        private val _fullPlayerVisible = MutableStateFlow(false)
        val fullPlayerVisible: StateFlow<Boolean> = _fullPlayerVisible.asStateFlow()

        private val _availability = MutableStateFlow<StationAvailability?>(null)
        val availability: StateFlow<StationAvailability?> = _availability.asStateFlow()

        private val recoveryPolicy = RecoveryPolicy()

        val castState: StateFlow<CastPlayerState> = castPlayerManager.castState

        private var loadJob: Job? = null
        private var availabilityJob: Job? = null
        private var controller: MediaController? = null
        private var attachedPlayer: Player? = null

        /** Emisora y dispositivo pendientes de reenviar al reconectar (bug 0032). */
        private var lostStation: StationDto? = null
        private var lostDeviceName: String? = null

        private val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (castPlayerManager.isCastConnected) return
                    if (profileController.applying.value) return
                    if (recoveryManager.status.value !is RecoveryState.Idle) return
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

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (castPlayerManager.isCastConnected) return
                    if (profileController.applying.value) return
                    if (recoveryManager.status.value !is RecoveryState.Idle) return
                    val current = activeStation() ?: return
                    _state.value = if (isPlaying) PlayerState.Playing(current) else PlayerState.Paused(current)
                    syncHolderToState()
                    syncCastState()
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    if (playWhenReady) {
                        recoveryManager.onPlaybackResumedBySystem()
                    }
                }
            }

        init {
            attachListener(engine.player)
            viewModelScope.launch {
                engine.playerFlow.drop(1).collectLatest { player ->
                    attachListener(player)
                }
            }
            connectController()
            syncFromHolder()
            observeRecovery()
            observeProfileApplying()
            observeCastNotices()
            observeCastReconnection()
        }

        private fun attachListener(player: Player) {
            attachedPlayer?.removeListener(listener)
            attachedPlayer = player
            player.addListener(listener)
        }

        @Suppress("LongMethod")
        private fun observeRecovery() {
            viewModelScope.launch {
                recoveryManager.status.collect { status ->
                    val station = activeStationHolder.station ?: _state.value.stationOrNull() ?: return@collect
                    when (status) {
                        is RecoveryState.Reconnecting ->
                            _state.value =
                                if (status.initial) {
                                    PlayerState.Buffering(
                                        station = station,
                                        retrying = true,
                                        waitingForNetwork = false,
                                    )
                                } else {
                                    PlayerState.Reconnecting(
                                        station = station,
                                        attempt = status.attempt,
                                        maxAttempts = status.maxAttempts,
                                        waitingForNetwork = false,
                                    )
                                }
                        is RecoveryState.WaitingForNetwork ->
                            _state.value =
                                if (status.initial) {
                                    PlayerState.Buffering(
                                        station = station,
                                        retrying = true,
                                        waitingForNetwork = true,
                                    )
                                } else {
                                    PlayerState.Reconnecting(
                                        station = station,
                                        attempt = 0,
                                        maxAttempts = recoveryPolicy.maxAttempts,
                                        waitingForNetwork = true,
                                    )
                                }
                        is RecoveryState.Failed ->
                            _state.value =
                                PlayerState.Error(
                                    station = station,
                                    message = status.message,
                                    action = status.action,
                                )
                        is RecoveryState.PausedAfterLoss ->
                            _state.value = PlayerState.Paused(station)
                        RecoveryState.Idle -> {
                            val current = _state.value
                            if (current is PlayerState.Reconnecting ||
                                (current is PlayerState.Buffering && current.retrying)
                            ) {
                                _state.value =
                                    if (engine.player.isPlaying) {
                                        PlayerState.Playing(station)
                                    } else {
                                        PlayerState.Buffering(station)
                                    }
                            }
                        }
                    }
                    syncHolderToState()
                    syncCastState()
                }
            }
        }

        private fun observeProfileApplying() {
            viewModelScope.launch {
                profileController.applying.collect { applying ->
                    val station = activeStationHolder.station ?: return@collect
                    if (applying) {
                        _state.value =
                            PlayerState.Reconnecting(
                                station = station,
                                attempt = 0,
                                maxAttempts = recoveryPolicy.maxAttempts,
                                applying = true,
                            )
                    } else {
                        val current = _state.value
                        if (current is PlayerState.Reconnecting && current.applying) {
                            _state.value =
                                if (engine.player.isPlaying) {
                                    PlayerState.Playing(station)
                                } else {
                                    PlayerState.Buffering(station)
                                }
                        }
                    }
                }
            }
        }

        private fun observeCastNotices() {
            viewModelScope.launch {
                castPlayerManager.notices.collect { notice ->
                    when (notice) {
                        is CastNotice.SessionLost -> showCastSessionLost(notice.deviceName)
                    }
                }
            }
        }

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

        fun play(station: StationDto) {
            loadJob?.cancel()
            availabilityJob?.cancel()
            lostStation = null
            lostDeviceName = null
            recoveryManager.onStationSelected(station)
            recoveryManager.onPlaybackResumedBySystem()
            resetPlaybackSession()
            _state.value = PlayerState.Buffering(station)
            syncHolderToState()
            syncCastState()
            val cached =
                _availability.value?.takeIf {
                    it.stationId == station.id && it.isFresh(SystemClock.elapsedRealtime())
                }
            if (cached != null && !cached.playable) {
                setError(station, PlaybackStatusReason.reasonToMessage(cached.reason))
                return
            }
            loadJob =
                viewModelScope.launch {
                    val availability = cached ?: checkStatus(station)
                    if (availability != null && !availability.playable) {
                        setError(station, PlaybackStatusReason.reasonToMessage(availability.reason))
                    } else {
                        startSource(station, resolveSource(station))
                    }
                }
        }

        /**
         * Precheck compartido por la ficha y el play.
         *
         * No cambia [PlayerState]; la ficha muestra el aviso local y el play
         * aplica el error si el usuario intenta reproducir.
         */
        fun cancelAvailability() {
            availabilityJob?.cancel()
            availabilityJob = null
            _availability.value = null
        }

        fun checkAvailability(station: StationDto) {
            val cached =
                _availability.value?.takeIf {
                    it.stationId == station.id && it.isFresh(SystemClock.elapsedRealtime())
                }
            if (cached != null) return
            availabilityJob?.cancel()
            if (_availability.value?.stationId != station.id) {
                _availability.value = null
            }
            availabilityJob =
                viewModelScope.launch {
                    checkStatus(station)
                }
        }

        private suspend fun checkStatus(station: StationDto): StationAvailability? {
            val result = withTimeoutOrNull(STATUS_TIMEOUT_MS) { playback.status(station.id) }
            val availability =
                if (result is ApiResult.Ok) {
                    StationAvailability(
                        stationId = station.id,
                        playable = result.value.playable,
                        reason = result.value.reason,
                        checkedAt = SystemClock.elapsedRealtime(),
                    )
                } else {
                    null
                }
            if (availability != null) {
                _availability.value = availability
            } else if (_availability.value?.stationId == station.id) {
                _availability.value = null
            }
            return availability
        }

        private suspend fun startSource(
            station: StationDto,
            source: PlaybackSource,
        ) {
            if (castPlayerManager.isCastConnected) {
                // Cast reproduce por la URL pública (0026): el servidor no
                // registra la escucha, así que no se inserta en el historial.
                castPlayerManager.connectToStation(station, source)
            } else {
                localPlaybackStarter.start(station)
            }
            syncCastState()
        }

        fun toggleMute() {
            volumeController.toggleMute()
        }

        fun openFullPlayer() {
            _fullPlayerVisible.value = true
        }

        fun closeFullPlayer() {
            _fullPlayerVisible.value = false
        }

        private fun activeStation(): StationDto? =
            (_state.value as? PlayerState.Playing)?.station
                ?: (_state.value as? PlayerState.Buffering)?.station
                ?: (_state.value as? PlayerState.Paused)?.station
                ?: (_state.value as? PlayerState.Reconnecting)?.station
                ?: (_state.value as? PlayerState.Error)?.station

        private fun effectiveState(): PlayerState =
            (castPlayerManager.castState.value as? CastPlayerState.Cast)?.playerState ?: _state.value

        private fun setError(
            station: StationDto?,
            message: String,
            action: PlayerErrorAction = PlayerErrorAction.RETRY,
        ) {
            _state.value = PlayerState.Error(station, message, action)
            syncHolderToState()
            syncCastState()
        }

        private fun resetPlaybackSession() {
            volumeController.resetMute()
        }

        private fun syncHolderToState() {
            val current = _state.value
            val holderState =
                when (current) {
                    is PlayerState.Playing -> PlayerStateType.PLAYING
                    is PlayerState.Paused -> PlayerStateType.PAUSED
                    is PlayerState.Buffering -> PlayerStateType.BUFFERING
                    is PlayerState.Reconnecting -> PlayerStateType.BUFFERING
                    is PlayerState.Error -> PlayerStateType.ERROR
                    PlayerState.Idle -> PlayerStateType.IDLE
                }
            activeStationHolder.update(current.stationOrNull(), holderState)
        }

        private fun syncCastState() {
            if (!castPlayerManager.isCastConnected) {
                castPlayerManager.updateLocalState(_state.value)
            }
        }

        fun cancelLoad() {
            loadJob?.cancel()
            availabilityJob?.cancel()
            _availability.value = null
            recoveryManager.cancel()
            if (effectiveState() is PlayerState.Buffering) {
                castPlayerManager.activePlayer.stop()
                castPlayerManager.activePlayer.clearMediaItems()
                _state.value = PlayerState.Idle
                activeStationHolder.clear()
                syncCastState()
            }
        }

        fun toggle() {
            val player = castPlayerManager.activePlayer
            when (val current = effectiveState()) {
                is PlayerState.Playing -> {
                    player.playWhenReady = false
                    if (!castPlayerManager.isCastConnected) recoveryManager.cancel()
                }
                is PlayerState.Paused -> {
                    val localNeedsRestart =
                        !castPlayerManager.isCastConnected &&
                            (engine.player.playbackState == Player.STATE_IDLE || engine.player.playerError != null)
                    if (localNeedsRestart) {
                        recoveryManager.manualRetry()
                    } else {
                        player.playWhenReady = true
                    }
                }
                else -> Unit
            }
            syncHolderToState()
            syncCastState()
        }

        fun stop() {
            loadJob?.cancel()
            availabilityJob?.cancel()
            _availability.value = null
            recoveryManager.cancel()
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

        fun retry() {
            val error =
                (effectiveState() as? PlayerState.Error)
                    ?: (_state.value as? PlayerState.Error)
                    ?: return
            if (error.action == PlayerErrorAction.EDIT_SERVER) return
            if (!recoveryManager.manualRetry()) {
                error.station?.let(::play)
            }
        }

        override fun onCleared() {
            loadJob?.cancel()
            availabilityJob?.cancel()
            attachedPlayer?.removeListener(listener)
            attachedPlayer = null
            castPlayerManager.release()
            controller?.release()
            super.onCleared()
        }

        private companion object {
            const val STATUS_TIMEOUT_MS = 2_000L
        }
    }
