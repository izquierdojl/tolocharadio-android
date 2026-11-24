package com.izquierdojl.tolocharadio.feature.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.data.local.HeadsetControlSettingsCache
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.playback.HeadsetControlSettings
import com.izquierdojl.tolocharadio.domain.playback.PlayerErrorAction
import com.izquierdojl.tolocharadio.domain.playback.RecoveryState
import com.izquierdojl.tolocharadio.domain.playback.SkipSource
import com.izquierdojl.tolocharadio.domain.playback.isTransportBuffering
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackController
import com.tolocharadio.ui.notification.NotificationNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Servicio de reproducción en segundo plano con notificación multimedia y foco
 * de audio (constitución II/IV).
 *
 * Desde la spec 0042 el player lo posee [PlaybackEngine]; el servicio cambia de
 * player con `MediaSession.setPlayer` al aplicar un perfil de buffer y NO libera
 * la instancia compartida en `onDestroy`.
 */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {
    @Inject
    lateinit var engine: PlaybackEngine

    @Inject
    lateinit var castPlayerManager: CastPlayerManager

    @Inject
    lateinit var volumeController: PlaybackVolumeController

    @Inject
    lateinit var profileController: PlaybackProfileController

    @Inject
    lateinit var prefs: InstancePrefs

    @Inject
    lateinit var localPlaybackStarter: LocalPlaybackStarter

    @Inject
    lateinit var activeStationHolder: ActiveStationHolder

    @Inject
    lateinit var recoveryManager: PlaybackRecoveryManager

    @Inject
    lateinit var recoveryAwarePlayer: RecoveryAwarePlayer

    @Inject
    lateinit var episodeController: EpisodePlaybackController

    @Inject
    lateinit var headsetControls: HeadsetControlsController

    @Inject
    lateinit var headsetSettingsCache: HeadsetControlSettingsCache

    private var session: MediaSession? = null
    private var episodeActive = false
    private var lastRecovery: RecoveryState = RecoveryState.Idle
    private var headsetSettings: HeadsetControlSettings = HeadsetControlSettings.DEFAULT
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        const val CUSTOM_COMMAND_STOP = "com.izquierdojl.tolocharadio.STOP"
        const val CUSTOM_COMMAND_MUTE = "com.izquierdojl.tolocharadio.MUTE"
        const val CUSTOM_COMMAND_RETRY = "com.izquierdojl.tolocharadio.RETRY"
        const val CUSTOM_COMMAND_SEEK_BACK = "com.izquierdojl.tolocharadio.SEEK_BACK"
        const val CUSTOM_COMMAND_SEEK_FORWARD = "com.izquierdojl.tolocharadio.SEEK_FORWARD"
        private const val TAG = "RadioPlaybackService"
    }

    private val stopButton by lazy {
        CommandButton.Builder(CommandButton.ICON_STOP)
            .setDisplayName("Stop")
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_STOP, Bundle.EMPTY))
            .build()
    }

    private val muteButton by lazy {
        CommandButton.Builder(CommandButton.ICON_VOLUME_OFF)
            .setDisplayName("Mute")
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_MUTE, Bundle.EMPTY))
            .build()
    }

    private val retryButton by lazy {
        CommandButton.Builder(CommandButton.ICON_PLAY)
            .setDisplayName("Reintentar")
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_RETRY, Bundle.EMPTY))
            .build()
    }

    private fun seekBackButton(seconds: Int): CommandButton =
        CommandButton.Builder(
            if (seconds == HeadsetControlSettings.DEFAULT_SKIP_BACK_SECONDS) {
                CommandButton.ICON_SKIP_BACK_15
            } else {
                CommandButton.ICON_SKIP_BACK
            },
        )
            .setSlots(CommandButton.SLOT_BACK)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SEEK_BACK, Bundle.EMPTY))
            .setDisplayName(getString(R.string.media_seek_back_seconds, seconds))
            .build()

    private fun seekForwardButton(seconds: Int): CommandButton =
        CommandButton.Builder(
            if (seconds == HeadsetControlSettings.DEFAULT_SKIP_FORWARD_SECONDS) {
                CommandButton.ICON_SKIP_FORWARD_30
            } else {
                CommandButton.ICON_SKIP_FORWARD
            },
        )
            .setSlots(CommandButton.SLOT_FORWARD)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SEEK_FORWARD, Bundle.EMPTY))
            .setDisplayName(getString(R.string.media_seek_forward_seconds, seconds))
            .build()

    private val sessionCallback =
        object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
                val sessionCommands =
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(SessionCommand(CUSTOM_COMMAND_STOP, Bundle.EMPTY))
                        .add(SessionCommand(CUSTOM_COMMAND_MUTE, Bundle.EMPTY))
                        .add(SessionCommand(CUSTOM_COMMAND_RETRY, Bundle.EMPTY))
                        .add(SessionCommand(CUSTOM_COMMAND_SEEK_BACK, Bundle.EMPTY))
                        .add(SessionCommand(CUSTOM_COMMAND_SEEK_FORWARD, Bundle.EMPTY))
                        .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): com.google.common.util.concurrent.ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    CUSTOM_COMMAND_STOP -> {
                        session.player.stop()
                        session.player.clearMediaItems()
                    }
                    CUSTOM_COMMAND_MUTE -> {
                        // Afecta a la salida activa (Cast o local), spec 0035 FR-007.
                        volumeController.toggleMute()
                    }
                    CUSTOM_COMMAND_RETRY -> {
                        recoveryManager.manualRetry()
                    }
                    CUSTOM_COMMAND_SEEK_BACK -> {
                        headsetControls.skipBack(SkipSource.ExplicitControl, "notification_skip")
                    }
                    CUSTOM_COMMAND_SEEK_FORWARD -> {
                        headsetControls.skipForward(SkipSource.ExplicitControl, "notification_skip")
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent,
            ): Boolean {
                @Suppress("DEPRECATION")
                val event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? KeyEvent ?: return false
                return headsetControls.onMediaButton(
                    keyCode = event.keyCode,
                    action = event.action,
                    repeatCount = event.repeatCount,
                )
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int,
            ): Int {
                val handled =
                    when (playerCommand) {
                        Player.COMMAND_SEEK_BACK,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        -> {
                            headsetControls.skipBack(SkipSource.ExplicitControl, "system_skip_back")
                            true
                        }

                        Player.COMMAND_SEEK_FORWARD,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        -> {
                            headsetControls.skipForward(SkipSource.ExplicitControl, "system_skip_forward")
                            true
                        }

                        else -> false
                    }
                return if (handled) SessionError.INFO_CANCELLED else SessionResult.RESULT_SUCCESS
            }
        }

    override fun onCreate() {
        super.onCreate()

        engine.ensureProfile(runBlocking { prefs.bufferProfile.first() })
        recoveryAwarePlayer.updateDelegate(engine.player)
        applySkipIncrements(engine.player)

        val sessionActivityIntent =
            Intent(this, com.izquierdojl.tolocharadio.MainActivity::class.java).apply {
                action = NotificationNavigation.NOTIFICATION_TAP_ACTION
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        val sessionActivityPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                sessionActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        session =
            MediaSession.Builder(this, recoveryAwarePlayer)
                .setCallback(sessionCallback)
                .setMediaButtonPreferences(listOf(stopButton, muteButton))
                .setSessionActivity(sessionActivityPendingIntent)
                .build()
        // Register MediaSession with CastPlayerManager for dynamic player switching
        castPlayerManager.setMediaSession(session!!)
        updateMediaButtons()
        recoveryManager.start()
        observeLocalPlayer()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.sierra_emblem_mono)
            },
        )

        serviceScope.launch {
            profileController.requests.collect { applyProfile(it) }
        }
        serviceScope.launch {
            recoveryManager.status.collect { status ->
                lastRecovery = status
                updateMediaButtons()
                recoveryAwarePlayer.setRecovering(status.isTransportBuffering())
            }
        }
        serviceScope.launch {
            episodeController.state.collect { state ->
                episodeActive = episodeController.isEpisodeActive()
                updateMediaButtons()
            }
        }
        serviceScope.launch {
            headsetSettingsCache.settings.collect { settings ->
                headsetSettings = settings
                applySkipIncrements(engine.player)
                updateMediaButtons()
            }
        }
    }

    /**
     * Un solo punto de cambio de player: el swap por tipo de contenido
     * (spec 0058) actualiza la sesión y libera el anterior después.
     */
    private fun observeLocalPlayer() {
        serviceScope.launch {
            engine.playerFlow.collect { player ->
                recoveryAwarePlayer.updateDelegate(player)
                applySkipIncrements(player)
                if (!castPlayerManager.isCastConnected) {
                    session?.setPlayer(recoveryAwarePlayer)
                }
                engine.takePendingRelease()?.let { old ->
                    old.stop()
                    old.release()
                }
            }
        }
    }

    private suspend fun applyProfile(profile: BufferProfile) {
        // El perfil es de radio: durante un episodio se aplicará al volver a
        // la radio (LocalPlaybackStarter.ensure) sin interrumpirlo.
        if (episodeController.isEpisodeActive()) return
        if (profile == engine.currentProfile) return
        profileController.setApplying(true)
        var startFailed = false
        try {
            val station = activeStationHolder.station
            val wasPlaying = engine.player.playWhenReady
            val castConnected = castPlayerManager.isCastConnected
            val swap = engine.swap(profile)
            swap.old.stop()
            swap.old.clearMediaItems()
            recoveryAwarePlayer.updateDelegate(swap.new)
            applySkipIncrements(swap.new)
            if (!castConnected) {
                session?.setPlayer(recoveryAwarePlayer)
                if (station != null) {
                    runCatching {
                        localPlaybackStarter.start(
                            station = station,
                            recordHistory = false,
                            playWhenReady = wasPlaying,
                        )
                    }.onFailure { error ->
                        startFailed = true
                        Log.w(TAG, "apply_profile_start_failed", error)
                    }
                }
            }
            swap.old.release()
        } finally {
            profileController.setApplying(false)
        }
        if (startFailed) {
            recoveryManager.onPlaybackStartFailed()
        }
    }

    private fun updateMediaButtons() {
        val retry = (lastRecovery as? RecoveryState.Failed)?.action == PlayerErrorAction.RETRY
        val layout =
            when (MediaButtonPolicy.layout(episodeActive = episodeActive, radioRetry = retry)) {
                MediaButtonLayout.EPISODE ->
                    listOf(
                        seekBackButton(headsetSettings.skipBackSeconds),
                        seekForwardButton(headsetSettings.skipForwardSeconds),
                        stopButton,
                        muteButton,
                    )
                MediaButtonLayout.RADIO_RETRY -> listOf(retryButton, stopButton, muteButton)
                MediaButtonLayout.RADIO -> listOf(stopButton, muteButton)
            }
        session?.setMediaButtonPreferences(layout)
    }

    /** Aplica los incrementos configurados al player local (spec 0060). */
    private fun applySkipIncrements(player: androidx.media3.exoplayer.ExoPlayer) {
        player.setSeekBackIncrementMs(headsetSettings.skipBackMs)
        player.setSeekForwardIncrementMs(headsetSettings.skipForwardMs)
    }

    /**
     * Control de ciclo de vida al eliminar la tarea (swipe de recents).
     * La notificación DEBE persistir si el player está pausado (spec 010, FR-002).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = engine.player
        if (player.playWhenReady || player.isPlaying) {
            return
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        serviceScope.cancel()
        recoveryManager.stop()
        session?.release()
        engine.player.stop()
        engine.player.clearMediaItems()
        super.onDestroy()
    }
}
