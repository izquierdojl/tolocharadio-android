package com.izquierdojl.tolocharadio.feature.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.tolocharadio.ui.notification.NotificationNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Servicio de reproducción en segundo plano con notificación
 * multimedia y foco de audio (constitución II/IV).
 *
 * Notificación rica tipo Pocket Casts con:
 * - Nombre de emisora (via MediaMetadata.title)
 * - Botón play/pause (default)
 * - Botón stop (custom command)
 * - Botón silenciar (custom command)
 * - Artwork de la emisora (via MediaMetadata.artworkUri)
 */
@AndroidEntryPoint
@UnstableApi
class RadioPlaybackService : MediaSessionService() {
    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var castPlayerManager: CastPlayerManager

    @Inject
    lateinit var volumeController: PlaybackVolumeController

    private var session: MediaSession? = null

    companion object {
        const val CUSTOM_COMMAND_STOP = "com.izquierdojl.tolocharadio.STOP"
        const val CUSTOM_COMMAND_MUTE = "com.izquierdojl.tolocharadio.MUTE"
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
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

    override fun onCreate() {
        super.onCreate()

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
            MediaSession.Builder(this, player)
                .setCallback(sessionCallback)
                .setCustomLayout(listOf(stopButton, muteButton))
                .setSessionActivity(sessionActivityPendingIntent)
                .build()
        // Register MediaSession with CastPlayerManager for dynamic player switching
        castPlayerManager.setMediaSession(session!!)
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.sierra_emblem_mono)
            },
        )
    }

    /**
     * Control de ciclo de vida al eliminar la tarea (swipe de recents).
     * La notificación DEBE persistir si el player está pausado (spec 010, FR-002).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (player.playWhenReady || player.isPlaying) {
            return
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        player.release()
        super.onDestroy()
    }
}
