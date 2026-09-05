package com.example.tolocharadio.feature.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Servicio de reproducción en segundo plano con notificación
 * multimedia y foco de audio (constitución II/IV).
 */
@AndroidEntryPoint
class RadioPlaybackService : MediaSessionService() {
    @Inject
    lateinit var player: ExoPlayer

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        player.release()
        super.onDestroy()
    }
}
