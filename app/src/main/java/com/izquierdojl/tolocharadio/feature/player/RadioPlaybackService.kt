package com.izquierdojl.tolocharadio.feature.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.playback.PlayerErrorAction
import com.izquierdojl.tolocharadio.domain.playback.RecoveryState
import com.izquierdojl.tolocharadio.domain.playback.isTransportBuffering
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

    private var session: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        const val CUSTOM_COMMAND_STOP = "com.izquierdojl.tolocharadio.STOP"
        const val CUSTOM_COMMAND_MUTE = "com.izquierdojl.tolocharadio.MUTE"
        const val CUSTOM_COMMAND_RETRY = "com.izquierdojl.tolocharadio.RETRY"
        private const val TAG = "RadioPlaybackService"
    }

    private val stopButton by lazy {
        CommandButton.Builder(CommandButton.ICON_STOP)
            .setDisplayName("Stop")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_STOP, Bundle.EMPTY))
            .build()
    }

    private val muteButton by lazy {
        CommandButton.Builder(CommandButton.ICON_VOLUME_OFF)
            .setDisplayName("Mute")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_MUTE, Bundle.EMPTY))
            .build()
    }

    private val retryButton by lazy {
        CommandButton.Builder(CommandButton.ICON_PLAY)
            .setDisplayName("Reintentar")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_RETRY, Bundle.EMPTY))
            .build()
    }

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
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

    override fun onCreate() {
        super.onCreate()

        engine.ensureProfile(runBlocking { prefs.bufferProfile.first() })
        recoveryAwarePlayer.updateDelegate(engine.player)

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
                .setCustomLayout(listOf(stopButton, muteButton))
                .setSessionActivity(sessionActivityPendingIntent)
                .build()
        // Register MediaSession with CastPlayerManager for dynamic player switching
        castPlayerManager.setMediaSession(session!!)
        recoveryManager.start()
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
                updateCustomLayout(status)
                recoveryAwarePlayer.setRecovering(status.isTransportBuffering())
            }
        }
    }

    private suspend fun applyProfile(profile: BufferProfile) {
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

    private fun updateCustomLayout(status: RecoveryState) {
        val layout =
            if (status is RecoveryState.Failed && status.action == PlayerErrorAction.RETRY) {
                listOf(retryButton, stopButton, muteButton)
            } else {
                listOf(stopButton, muteButton)
            }
        session?.setCustomLayout(layout)
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
