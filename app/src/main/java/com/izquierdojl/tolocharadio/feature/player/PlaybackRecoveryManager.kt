package com.izquierdojl.tolocharadio.feature.player

import android.os.SystemClock
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.ConnectivityMonitor
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.auth.AuthenticateServerUseCase
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import com.izquierdojl.tolocharadio.domain.playback.PlayerErrorAction
import com.izquierdojl.tolocharadio.domain.playback.RecoveryPolicy
import com.izquierdojl.tolocharadio.domain.playback.RecoveryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detecta cortes y reintenta la reproducción fuera del ViewModel (spec 0042,
 * FR-001..FR-009/FR-014..FR-019).
 *
 * Vive en un scope Main porque ExoPlayer debe tocarse en su looper principal.
 */
@Singleton
@Suppress("LongParameterList", "TooManyFunctions")
class PlaybackRecoveryManager
    @Inject
    constructor(
        private val engine: PlaybackEngine,
        private val connectivity: ConnectivityMonitor,
        private val prefs: InstancePrefs,
        private val activeStationHolder: ActiveStationHolder,
        private val localPlaybackStarter: LocalPlaybackStarter,
        private val authenticateServer: AuthenticateServerUseCase,
        private val castPlayerManager: CastPlayerManager,
        private val profileController: PlaybackProfileController,
        private val logger: PlaybackRecoveryLogger,
        private val scheduler: RecoveryScheduler,
    ) {
        private val policy = RecoveryPolicy()
        private val scope: CoroutineScope
            get() = scheduler.scope

        private val _status = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
        val status: StateFlow<RecoveryState> = _status.asStateFlow()

        private var attachedPlayer: ExoPlayer? = null
        private var recoveryJob: Job? = null
        private var watchdogJob: Job? = null
        private var userPaused = false
        private var suppressUserPauseCancel = false
        private var hasPlayedOnce = false
        private var lastPosition = 0L
        private var stalledTicks = 0

        private val listener =
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (castPlayerManager.isCastConnected || userPaused) return
                    val cause =
                        if (!connectivity.isOnline.value) {
                            InterruptionCause.NO_CONNECTIVITY
                        } else {
                            PlaybackErrorClassifier.classify(error)
                        }
                    startRecovery(cause)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            hasPlayedOnce = true
                            lastPosition = engine.player.currentPosition
                            stalledTicks = 0
                            clearRecoveryIfPlaying()
                            startWatchdog()
                        }
                        Player.STATE_BUFFERING -> startWatchdog()
                        Player.STATE_IDLE, Player.STATE_ENDED -> {
                            stalledTicks = 0
                            watchdogJob?.cancel()
                            watchdogJob = null
                        }
                    }
                }

                override fun onMediaItemTransition(
                    mediaItem: androidx.media3.common.MediaItem?,
                    reason: Int,
                ) {
                    // T070: una emisora nueva vuelve a usar el umbral inicial de 10 s.
                    hasPlayedOnce = false
                    lastPosition = engine.player.currentPosition
                    stalledTicks = 0
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        hasPlayedOnce = true
                        lastPosition = engine.player.currentPosition
                        stalledTicks = 0
                        clearRecoveryIfPlaying()
                    } else if (engine.player.playWhenReady && _status.value is RecoveryState.Idle) {
                        startWatchdog()
                    }
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    if (playWhenReady) {
                        userPaused = false
                        startWatchdog()
                        return
                    }
                    when (reason) {
                        Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                            if (!suppressUserPauseCancel) cancel()
                        }
                        Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
                        Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY,
                        -> {
                            watchdogJob?.cancel()
                            watchdogJob = null
                        }
                    }
                }
            }

        init {
            scope.launch {
                engine.playerFlow.collectLatest { player ->
                    attachedPlayer?.removeListener(listener)
                    attachedPlayer = player
                    player.addListener(listener)
                    hasPlayedOnce = false
                    lastPosition = player.currentPosition
                    stalledTicks = 0
                    startWatchdog()
                }
            }
            scope.launch {
                connectivity.isOnline.collect { online ->
                    val waiting = _status.value as? RecoveryState.WaitingForNetwork ?: return@collect
                    if (!online) return@collect
                    if (prefs.resumeOnConnectivity.first()) {
                        startRecovery(waiting.cause)
                    } else {
                        pauseAfterLoss(waiting.stationId)
                    }
                }
            }
        }

        /** Cancela un ciclo y deja el manager inactivo. */
        fun cancel() {
            userPaused = true
            recoveryJob?.cancel()
            recoveryJob = null
            watchdogJob?.cancel()
            watchdogJob = null
            _status.value = RecoveryState.Idle
        }

        /** Reintento manual desde la UI o la notificación. */
        fun manualRetry(): Boolean {
            val station = activeStationHolder.station ?: return false
            if (castPlayerManager.isCastConnected) return false
            userPaused = false
            engine.player.playWhenReady = true
            if (recoveryJob?.isActive == true) return true
            val cause =
                if (!connectivity.isOnline.value) {
                    InterruptionCause.NO_CONNECTIVITY
                } else {
                    causeForManualRetry(_status.value)
                }
            startRecovery(cause)
            return true
        }

        /** Causa que debe conservar un reintento manual (T067). */
        internal fun causeForManualRetry(status: RecoveryState): InterruptionCause =
            when (status) {
                is RecoveryState.Failed -> status.cause
                is RecoveryState.WaitingForNetwork -> status.cause
                else -> InterruptionCause.UNKNOWN
            }

        /** El servicio no pudo arrancar la emisora al aplicar un perfil (T068). */
        fun onPlaybackStartFailed() {
            startRecovery(InterruptionCause.NETWORK_ERROR)
        }

        internal data class WatchdogSnapshot(
            val hasPlayedOnce: Boolean,
            val lastPosition: Long,
            val stalledTicks: Int,
        )

        internal fun watchdogSnapshot(): WatchdogSnapshot = WatchdogSnapshot(hasPlayedOnce, lastPosition, stalledTicks)

        /** El sistema reanudó el player (nueva reproducción o foco recuperado). */
        fun onPlaybackResumedBySystem() {
            userPaused = false
            startWatchdog()
        }

        private fun startRecovery(cause: InterruptionCause) {
            if (userPaused || castPlayerManager.isCastConnected || profileController.applying.value) return
            if (recoveryJob?.isActive == true) return
            val station = activeStationHolder.station ?: return
            if (!connectivity.isOnline.value) {
                _status.value = RecoveryState.WaitingForNetwork(station.id, cause)
                activeStationHolder.update(station, PlayerStateType.BUFFERING)
                return
            }
            recoveryJob =
                scope.launch {
                    runAttempts(station, cause)
                }
        }

        @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount")
        private suspend fun runAttempts(
            station: StationDto,
            initialCause: InterruptionCause,
        ) {
            val startedAt = SystemClock.elapsedRealtime()
            var cause = initialCause
            var attempt = 0
            activeStationHolder.update(station, PlayerStateType.BUFFERING)
            _status.value = RecoveryState.Reconnecting(station.id, 0, policy.maxAttempts, cause)
            while (attempt < policy.maxAttempts &&
                SystemClock.elapsedRealtime() - startedAt < policy.totalTimeoutMs
            ) {
                if (userPaused) {
                    _status.value = RecoveryState.Idle
                    return
                }
                if (!connectivity.isOnline.value) {
                    _status.value = RecoveryState.WaitingForNetwork(station.id, cause)
                    while (!connectivity.isOnline.value && !userPaused) {
                        delay(CONNECTIVITY_POLL_MS)
                    }
                    if (userPaused) {
                        _status.value = RecoveryState.Idle
                        return
                    }
                    if (!prefs.resumeOnConnectivity.first()) {
                        pauseAfterLoss(station.id)
                        return
                    }
                }
                attempt++
                _status.value = RecoveryState.Reconnecting(station.id, attempt, policy.maxAttempts, cause)
                logger.attempt(cause, attempt, station.id)
                val wait = policy.delayForAttempt(attempt)
                if (wait > 0) delay(wait)
                if (userPaused) {
                    _status.value = RecoveryState.Idle
                    return
                }
                if (cause == InterruptionCause.AUTH_ERROR) {
                    when (authenticateServer()) {
                        is ApiResult.Err -> {
                            fail(
                                station.id,
                                InterruptionCause.AUTH_ERROR,
                                "La sesión del servidor ha caducado. Edita el servidor para continuar.",
                                PlayerErrorAction.EDIT_SERVER,
                                startedAt,
                                attempt,
                            )
                            return
                        }
                        is ApiResult.Ok -> Unit
                    }
                }
                runCatching {
                    localPlaybackStarter.start(station, recordHistory = false)
                }.onFailure {
                    cause =
                        if (connectivity.isOnline.value) {
                            InterruptionCause.NETWORK_ERROR
                        } else {
                            InterruptionCause.NO_CONNECTIVITY
                        }
                }
                if (awaitReady(policy.attemptTimeoutMs)) {
                    _status.value = RecoveryState.Idle
                    activeStationHolder.update(station, PlayerStateType.PLAYING)
                    logger.success(cause, attempt, station.id, SystemClock.elapsedRealtime() - startedAt)
                    return
                }
                if (!connectivity.isOnline.value) {
                    cause = InterruptionCause.NO_CONNECTIVITY
                }
            }
            val message =
                when (cause) {
                    InterruptionCause.AUTH_ERROR ->
                        "La sesión del servidor ha caducado. Edita el servidor para continuar."
                    InterruptionCause.STATION_UNAVAILABLE ->
                        "La emisora no está disponible en este momento."
                    else ->
                        "No se pudo recuperar la conexión. Comprueba tu red e inténtalo de nuevo."
                }
            val action =
                if (cause == InterruptionCause.AUTH_ERROR) {
                    PlayerErrorAction.EDIT_SERVER
                } else {
                    PlayerErrorAction.RETRY
                }
            fail(station.id, cause, message, action, startedAt, attempt)
        }

        private suspend fun awaitReady(timeoutMs: Long): Boolean {
            val deadline = SystemClock.elapsedRealtime() + timeoutMs
            while (SystemClock.elapsedRealtime() < deadline) {
                if (userPaused) return false
                if (engine.player.isPlaying || engine.player.playbackState == Player.STATE_READY) return true
                delay(READY_POLL_MS)
            }
            return false
        }

        private fun pauseAfterLoss(stationId: String) {
            userPaused = true
            suppressUserPauseCancel = true
            engine.player.playWhenReady = false
            suppressUserPauseCancel = false
            recoveryJob?.cancel()
            recoveryJob = null
            watchdogJob?.cancel()
            watchdogJob = null
            _status.value = RecoveryState.PausedAfterLoss(stationId)
            activeStationHolder.update(activeStationHolder.station, PlayerStateType.PAUSED)
        }

        private fun fail(
            stationId: String,
            cause: InterruptionCause,
            message: String,
            action: PlayerErrorAction,
            startedAt: Long,
            attempts: Int,
        ) {
            _status.value = RecoveryState.Failed(stationId, cause, message, action)
            activeStationHolder.update(activeStationHolder.station, PlayerStateType.ERROR)
            logger.failure(cause, attempts, stationId, SystemClock.elapsedRealtime() - startedAt)
        }

        private fun clearRecoveryIfPlaying() {
            if (_status.value !is RecoveryState.Idle) {
                _status.value = RecoveryState.Idle
                activeStationHolder.station?.let { activeStationHolder.update(it, PlayerStateType.PLAYING) }
            }
        }

        @Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
        private fun startWatchdog() {
            if (!scheduler.watchdogEnabled) return
            if (watchdogJob?.isActive == true) return
            watchdogJob =
                scope.launch {
                    lastPosition = engine.player.currentPosition
                    while (isActive) {
                        delay(WATCHDOG_INTERVAL_MS)
                        val player = engine.player
                        val shouldPauseWatchdog =
                            castPlayerManager.isCastConnected ||
                                profileController.applying.value ||
                                !player.playWhenReady ||
                                userPaused ||
                                recoveryJob?.isActive == true
                        if (shouldPauseWatchdog) {
                            stalledTicks = 0
                            lastPosition = player.currentPosition
                            continue
                        }
                        val position = player.currentPosition
                        if (position == lastPosition &&
                            player.playbackState != Player.STATE_IDLE &&
                            player.playbackState != Player.STATE_ENDED
                        ) {
                            stalledTicks++
                        } else {
                            stalledTicks = 0
                            lastPosition = position
                        }
                        val threshold = if (hasPlayedOnce) STALL_THRESHOLD_TICKS else INITIAL_THRESHOLD_TICKS
                        if (stalledTicks >= threshold) {
                            startRecovery(
                                if (connectivity.isOnline.value) {
                                    InterruptionCause.STALL
                                } else {
                                    InterruptionCause.NO_CONNECTIVITY
                                },
                            )
                            break
                        }
                    }
                }
        }

        private companion object {
            const val WATCHDOG_INTERVAL_MS = 1_000L
            const val CONNECTIVITY_POLL_MS = 250L
            const val READY_POLL_MS = 100L
            const val STALL_THRESHOLD_TICKS = 5
            const val INITIAL_THRESHOLD_TICKS = 10
        }
    }
