package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.ConnectivityMonitor
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.auth.AuthenticateServerUseCase
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFailure
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFeatureFlags
import com.izquierdojl.tolocharadio.domain.playback.PlaybackMessages
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoutePolicy
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
 * Detecta cortes y reintenta la reproducción fuera del ViewModel
 * (spec 0042/0044).
 *
 * El servicio controla su ciclo con [start]/[stop]. Vive en un scope Main
 * porque ExoPlayer debe tocarse en su looper principal.
 */
@Singleton
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class PlaybackRecoveryManager
    @Inject
    constructor(
        private val engine: PlaybackEngine,
        private val connectivity: ConnectivityMonitor,
        private val prefs: InstancePrefs,
        private val activeStationHolder: ActiveStationHolder,
        private val fallbackController: PlaybackFallbackController,
        private val authenticateServer: AuthenticateServerUseCase,
        private val castPlayerManager: CastPlayerManager,
        private val profileController: PlaybackProfileController,
        private val logger: PlaybackRecoveryLogger,
        private val scheduler: RecoveryScheduler,
        private val flags: PlaybackFeatureFlags,
        private val sessionState: PlaybackSessionState,
        private val session: SessionManager,
        private val clock: PlaybackClock,
    ) {
        private val policy = RecoveryPolicy()
        private val scope: CoroutineScope
            get() = scheduler.scope

        private val _status = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
        val status: StateFlow<RecoveryState> = _status.asStateFlow()

        private val collectorJobs = mutableListOf<Job>()
        private var started = false
        private var attachedPlayer: ExoPlayer? = null
        private var recoveryJob: Job? = null
        private var watchdogJob: Job? = null
        private var stabilityJob: Job? = null
        private var userPaused = false
        private var suppressUserPauseCancel = false
        private var hasPlayedOnce = false
        private var lastPosition = 0L
        private var stalledTicks = 0
        private var fallbackAttempted = false
        private var usedAttempts = 0
        private var stableSince: Long? = null
        private var lastFailure: PlaybackFailure? = null

        private val listener =
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (castPlayerManager.isCastConnected || userPaused) return
                    if (profileController.applying.value) return
                    cancelStabilityWindow()
                    val failure = PlaybackErrorClassifier.classify(error, sessionState.route)
                    lastFailure = failure
                    val decision =
                        PlaybackRoutePolicy.decide(
                            route = sessionState.route,
                            cause = failure.cause,
                            fallbackAttempted = fallbackAttempted,
                            policy = policy,
                        )
                    if (decision is com.izquierdojl.tolocharadio.domain.playback.FallbackDecision.UseLegacy) {
                        fallbackAttempted = true
                        val from = sessionState.route
                        sessionState.useLegacyRoute()
                        activeStationHolder.station?.let {
                            logger.fallback(it.id, from, PlaybackRoute.LEGACY, failure, usedAttempts + 1)
                        }
                        recoveryJob?.cancel()
                        recoveryJob = null
                        if (decision.maxAttempts == 1) {
                            startFallbackAttempt(failure)
                        } else {
                            startRecovery(failure)
                        }
                        return
                    }
                    if (recoveryJob?.isActive == true) return
                    startRecovery(failure)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            lastPosition = engine.player.currentPosition
                            stalledTicks = 0
                            clearRecoveryIfPlaying()
                            startWatchdog()
                        }
                        Player.STATE_BUFFERING -> startWatchdog()
                        Player.STATE_ENDED -> {
                            stalledTicks = 0
                            watchdogJob?.cancel()
                            watchdogJob = null
                            if (hasPlayedOnce && !userPaused && !castPlayerManager.isCastConnected) {
                                startRecovery(PlaybackFailure(InterruptionCause.STREAM_ENDED))
                            }
                        }
                        Player.STATE_IDLE -> {
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
                        startStabilityWindow()
                        startWatchdog()
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
                        -> cancel()
                    }
                }
            }

        /** Arranca los colectores; lo llama el servicio en `onCreate`. */
        fun start() {
            if (started) return
            started = true
            userPaused = false
            collectorJobs +=
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
            collectorJobs +=
                scope.launch {
                    connectivity.isOnline.collect { online ->
                        val waiting = _status.value as? RecoveryState.WaitingForNetwork ?: return@collect
                        if (!online) return@collect
                        if (prefs.resumeOnConnectivity.first()) {
                            startRecovery(PlaybackFailure(waiting.cause))
                        } else {
                            pauseAfterLoss(waiting.stationId)
                        }
                    }
                }
            collectorJobs +=
                scope.launch {
                    connectivity.networkChanges.collect {
                        if (castPlayerManager.isCastConnected ||
                            userPaused ||
                            !connectivity.isOnline.value
                        ) {
                            return@collect
                        }
                        if (profileController.applying.value || recoveryJob?.isActive == true) return@collect
                        val station = activeStationHolder.station ?: return@collect
                        if (engine.player.playWhenReady || engine.player.isPlaying) {
                            logger.attempt(
                                PlaybackFailure(InterruptionCause.NETWORK_CHANGED),
                                usedAttempts + 1,
                                station.id,
                            )
                            startRecovery(PlaybackFailure(InterruptionCause.NETWORK_CHANGED))
                        }
                    }
                }
        }

        /** Detiene colectores y trabajos; lo llama el servicio en `onDestroy`. */
        fun stop() {
            started = false
            userPaused = true
            collectorJobs.forEach { it.cancel() }
            collectorJobs.clear()
            attachedPlayer?.removeListener(listener)
            attachedPlayer = null
            cancel()
        }

        /** Cancela un ciclo y deja el manager inactivo. */
        fun cancel() {
            userPaused = true
            recoveryJob?.cancel()
            recoveryJob = null
            watchdogJob?.cancel()
            watchdogJob = null
            stabilityJob?.cancel()
            stabilityJob = null
            stableSince = null
            _status.value = RecoveryState.Idle
        }

        /** Reintento manual desde la UI o la notificación. */
        fun manualRetry(): Boolean {
            val station = activeStationHolder.station ?: return false
            if (castPlayerManager.isCastConnected) return false
            userPaused = false
            engine.player.playWhenReady = true
            if (recoveryJob?.isActive == true) return true
            usedAttempts = 0
            val cause =
                if (!connectivity.isOnline.value) {
                    InterruptionCause.NO_CONNECTIVITY
                } else {
                    causeForManualRetry(_status.value)
                }
            startRecovery(PlaybackFailure(cause))
            return true
        }

        /** Causa que debe conservar un reintento manual. */
        internal fun causeForManualRetry(status: RecoveryState): InterruptionCause =
            when (status) {
                is RecoveryState.Failed -> status.cause
                is RecoveryState.WaitingForNetwork -> status.cause
                else -> InterruptionCause.UNKNOWN
            }

        /** El servicio no pudo arrancar la emisora al aplicar un perfil. */
        fun onPlaybackStartFailed() {
            startRecovery(PlaybackFailure(InterruptionCause.NETWORK_ERROR))
        }

        internal data class WatchdogSnapshot(
            val hasPlayedOnce: Boolean,
            val lastPosition: Long,
            val stalledTicks: Int,
        )

        internal fun watchdogSnapshot(): WatchdogSnapshot = WatchdogSnapshot(hasPlayedOnce, lastPosition, stalledTicks)

        internal data class BudgetSnapshot(
            val usedAttempts: Int,
            val stableSince: Long?,
        )

        internal fun budgetSnapshot(): BudgetSnapshot = BudgetSnapshot(usedAttempts, stableSince)

        /** El sistema reanudó el player (nueva reproducción o foco recuperado). */
        fun onPlaybackResumedBySystem() {
            userPaused = false
            startWatchdog()
        }

        /** Prepara una nueva emisora: ruta, presupuesto y fallback vuelven a cero. */
        fun onStationSelected(station: StationDto) {
            cancel()
            userPaused = false
            sessionState.resetRoute(flags.defaultRoute)
            fallbackAttempted = false
            usedAttempts = 0
            stableSince = null
            hasPlayedOnce = false
            lastFailure = null
            activeStationHolder.update(station, PlayerStateType.BUFFERING)
        }

        private fun startFallbackAttempt(failure: PlaybackFailure) {
            cancelStabilityWindow()
            if (externalPlaybackActive()) return
            val station = activeStationHolder.station ?: return
            if (recoveryJob?.isActive == true) return
            recoveryJob =
                scope.launch {
                    if (stopRecoveryForExternalOwner()) return@launch
                    val startedAt = clock.nowMs()
                    activeStationHolder.update(station, PlayerStateType.BUFFERING)
                    usedAttempts = 1
                    _status.value =
                        RecoveryState.Reconnecting(
                            stationId = station.id,
                            attempt = 1,
                            maxAttempts = 1,
                            cause = failure.cause,
                            initial = !hasPlayedOnce,
                        )
                    logger.attempt(failure, 1, station.id)
                    val sequence = sessionState.nextSequence()
                    val result =
                        fallbackController.start(
                            station = station,
                            route = PlaybackRoute.LEGACY,
                            sequence = sequence,
                            recordHistory = false,
                        )
                    if (stopRecoveryForExternalOwner()) return@launch
                    if (result.isSuccess && awaitReady(policy.attemptTimeoutMs)) {
                        if (stopRecoveryForExternalOwner()) return@launch
                        _status.value = RecoveryState.Idle
                        activeStationHolder.update(station, PlayerStateType.PLAYING)
                        logger.success(failure, 1, station.id, clock.nowMs() - startedAt)
                        startStabilityWindow()
                        return@launch
                    }
                    if (stopRecoveryForExternalOwner()) return@launch
                    val actual = lastFailure ?: failure
                    val message =
                        if (actual.cause == InterruptionCause.UNSUPPORTED_FORMAT ||
                            actual.cause == InterruptionCause.DECODER_ERROR
                        ) {
                            PlaybackMessages.UNSUPPORTED_FORMAT
                        } else {
                            PlaybackMessages.RECONNECTION_FAILED
                        }
                    fail(station.id, actual.cause, message, PlayerErrorAction.RETRY, startedAt, 1)
                }
        }

        @Suppress("ReturnCount")
        private fun startRecovery(failure: PlaybackFailure) {
            if (userPaused || castPlayerManager.isCastConnected || profileController.applying.value) return
            cancelStabilityWindow()
            if (recoveryJob?.isActive == true) return
            val station = activeStationHolder.station ?: return
            if (sessionState.route == PlaybackRoute.LEGACY && failure.cause == InterruptionCause.UNSUPPORTED_FORMAT) {
                fail(
                    station.id,
                    failure.cause,
                    PlaybackMessages.UNSUPPORTED_FORMAT,
                    PlayerErrorAction.RETRY,
                    clock.nowMs(),
                    usedAttempts,
                )
                return
            }
            if (!connectivity.isOnline.value) {
                _status.value = RecoveryState.WaitingForNetwork(station.id, failure.cause, initial = !hasPlayedOnce)
                activeStationHolder.update(station, PlayerStateType.BUFFERING)
                return
            }
            if (usedAttempts >= policy.maxAttempts) {
                fail(
                    station.id,
                    failure.cause,
                    messageFor(failure.cause),
                    actionFor(failure.cause),
                    clock.nowMs(),
                    usedAttempts,
                )
                return
            }
            recoveryJob =
                scope.launch {
                    runAttempts(station, failure)
                }
        }

        @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount", "LoopWithTooManyJumpStatements")
        private suspend fun runAttempts(
            station: StationDto,
            initialFailure: PlaybackFailure,
        ) {
            val startedAt = clock.nowMs()
            var cause = initialFailure.cause
            activeStationHolder.update(station, PlayerStateType.BUFFERING)
            _status.value =
                RecoveryState.Reconnecting(
                    stationId = station.id,
                    attempt = usedAttempts,
                    maxAttempts = policy.maxAttempts,
                    cause = cause,
                    initial = !hasPlayedOnce,
                )
            while (usedAttempts < policy.maxAttempts &&
                clock.nowMs() - startedAt < policy.totalTimeoutMs
            ) {
                if (userPaused) {
                    _status.value = RecoveryState.Idle
                    return
                }
                if (stopRecoveryForExternalOwner()) return
                if (!connectivity.isOnline.value) {
                    _status.value = RecoveryState.WaitingForNetwork(station.id, cause, initial = !hasPlayedOnce)
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
                if (stopRecoveryForExternalOwner()) return
                if (cause == InterruptionCause.AUTH_ERROR || session.isAccessTokenExpiring()) {
                    when (authenticateServer()) {
                        is ApiResult.Err -> {
                            fail(
                                station.id,
                                InterruptionCause.AUTH_ERROR,
                                PlaybackMessages.AUTH_EXPIRED,
                                PlayerErrorAction.EDIT_SERVER,
                                startedAt,
                                usedAttempts,
                            )
                            return
                        }
                        is ApiResult.Ok -> Unit
                    }
                    if (userPaused) {
                        _status.value = RecoveryState.Idle
                        return
                    }
                    if (stopRecoveryForExternalOwner()) return
                    if (!connectivity.isOnline.value) {
                        cause = InterruptionCause.NO_CONNECTIVITY
                        continue
                    }
                }
                usedAttempts++
                _status.value =
                    RecoveryState.Reconnecting(
                        stationId = station.id,
                        attempt = usedAttempts,
                        maxAttempts = policy.maxAttempts,
                        cause = cause,
                        initial = !hasPlayedOnce,
                    )
                logger.attempt(lastFailure ?: PlaybackFailure(cause), usedAttempts, station.id)
                val wait = policy.delayForAttempt(usedAttempts)
                if (wait > 0) delay(wait)
                if (userPaused) {
                    _status.value = RecoveryState.Idle
                    return
                }
                if (stopRecoveryForExternalOwner()) return
                lastFailure = null
                val sequence = sessionState.nextSequence()
                val result = fallbackController.start(station, sessionState.route, sequence, recordHistory = false)
                if (stopRecoveryForExternalOwner()) return
                if (result.isFailure) {
                    cause =
                        if (connectivity.isOnline.value) {
                            InterruptionCause.NETWORK_ERROR
                        } else {
                            InterruptionCause.NO_CONNECTIVITY
                        }
                }
                if (awaitReady(policy.attemptTimeoutMs)) {
                    if (stopRecoveryForExternalOwner()) return
                    _status.value = RecoveryState.Idle
                    activeStationHolder.update(station, PlayerStateType.PLAYING)
                    logger.success(
                        lastFailure ?: PlaybackFailure(cause),
                        usedAttempts,
                        station.id,
                        clock.nowMs() - startedAt,
                    )
                    startStabilityWindow()
                    return
                }
                if (stopRecoveryForExternalOwner()) return
                val failure =
                    lastFailure
                        ?: PlaybackFailure(
                            if (connectivity.isOnline.value) {
                                InterruptionCause.NETWORK_ERROR
                            } else {
                                InterruptionCause.NO_CONNECTIVITY
                            },
                        )
                cause = failure.cause
                if (sessionState.route == PlaybackRoute.LEGACY &&
                    (cause == InterruptionCause.UNSUPPORTED_FORMAT || cause == InterruptionCause.DECODER_ERROR)
                ) {
                    fail(
                        station.id,
                        cause,
                        PlaybackMessages.UNSUPPORTED_FORMAT,
                        PlayerErrorAction.RETRY,
                        startedAt,
                        usedAttempts,
                    )
                    return
                }
                if (!connectivity.isOnline.value) cause = InterruptionCause.NO_CONNECTIVITY
            }
            fail(
                station.id,
                cause,
                messageFor(cause),
                actionFor(cause),
                startedAt,
                usedAttempts,
            )
        }

        @Suppress("ReturnCount")
        private suspend fun awaitReady(timeoutMs: Long): Boolean {
            val deadline = clock.nowMs() + timeoutMs
            while (clock.nowMs() < deadline) {
                if (userPaused) return false
                if (externalPlaybackActive()) return false
                if (engine.player.isPlaying || engine.player.playbackState == Player.STATE_READY) return true
                delay(READY_POLL_MS)
            }
            return false
        }

        private fun externalPlaybackActive(): Boolean {
            return castPlayerManager.isCastConnected || profileController.applying.value
        }

        /** Detiene el audio local si Cast tomó el control y cierra el ciclo. */
        private fun stopRecoveryForExternalOwner(): Boolean {
            if (castPlayerManager.isCastConnected) {
                engine.player.playWhenReady = false
                _status.value = RecoveryState.Idle
                return true
            }
            if (profileController.applying.value) {
                _status.value = RecoveryState.Idle
                return true
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
            logger.failure(lastFailure ?: PlaybackFailure(cause), attempts, stationId, clock.nowMs() - startedAt)
        }

        private fun messageFor(cause: InterruptionCause): String =
            when (cause) {
                InterruptionCause.AUTH_ERROR -> PlaybackMessages.AUTH_EXPIRED
                InterruptionCause.STATION_UNAVAILABLE -> PlaybackMessages.STATION_UNAVAILABLE
                InterruptionCause.UNSUPPORTED_FORMAT,
                InterruptionCause.DECODER_ERROR,
                -> PlaybackMessages.UNSUPPORTED_FORMAT
                else -> PlaybackMessages.RECONNECTION_FAILED
            }

        private fun actionFor(cause: InterruptionCause): PlayerErrorAction =
            if (cause == InterruptionCause.AUTH_ERROR) PlayerErrorAction.EDIT_SERVER else PlayerErrorAction.RETRY

        private fun clearRecoveryIfPlaying() {
            if (_status.value !is RecoveryState.Idle) {
                _status.value = RecoveryState.Idle
                activeStationHolder.station?.let { activeStationHolder.update(it, PlayerStateType.PLAYING) }
            }
        }

        private fun cancelStabilityWindow() {
            stabilityJob?.cancel()
            stabilityJob = null
            stableSince = null
        }

        private fun startStabilityWindow() {
            stableSince = clock.nowMs()
            stabilityJob?.cancel()
            stabilityJob =
                scope.launch {
                    delay(policy.stableWindowMs)
                    if (engine.player.isPlaying) {
                        usedAttempts = 0
                        stableSince = null
                    }
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
                        if (stalledTicks >= STALL_THRESHOLD_TICKS) {
                            startRecovery(
                                PlaybackFailure(
                                    if (connectivity.isOnline.value) {
                                        InterruptionCause.STALL
                                    } else {
                                        InterruptionCause.NO_CONNECTIVITY
                                    },
                                ),
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
            const val STALL_THRESHOLD_TICKS = 15
        }
    }
