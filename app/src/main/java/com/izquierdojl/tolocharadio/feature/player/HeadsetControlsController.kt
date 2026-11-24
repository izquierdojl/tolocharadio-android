package com.izquierdojl.tolocharadio.feature.player

import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import androidx.media3.common.Player
import com.izquierdojl.tolocharadio.data.local.HeadsetControlSettingsCache
import com.izquierdojl.tolocharadio.domain.playback.HeadsetAction
import com.izquierdojl.tolocharadio.domain.playback.HeadsetCommand
import com.izquierdojl.tolocharadio.domain.playback.HeadsetControlPolicy
import com.izquierdojl.tolocharadio.domain.playback.PlaybackContent
import com.izquierdojl.tolocharadio.domain.playback.SkipSource
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ejecuta los saltos de los controles de auricular (spec 0060).
 *
 * Centraliza el gating común (episodio activo, modo de audio normal, sin
 * supresión por foco), la distinción entre gestos del auricular y controles
 * explícitos, y el recorte de la posición al rango del episodio.
 *
 * Hilos: se invoca desde el hilo principal (servicio, Activity y session
 * callback).
 */
@Singleton
class HeadsetControlsController
    @Inject
    constructor(
        private val episodeController: EpisodePlaybackController,
        private val settingsCache: HeadsetControlSettingsCache,
        private val engine: PlaybackEngine,
        private val audioManager: AudioManager,
    ) {
        /**
         * Indica si un salto desde [source] es posible ahora.
         *
         * `HeadsetGesture` exige además que el interruptor de gestos esté
         * activo; `ExplicitControl` no depende de ese interruptor (FR-012).
         */
        fun canSkip(source: SkipSource): Boolean {
            val episodeState =
                episodeController.state.value as? EpisodePlaybackController.State.Active ?: return false
            if (source == SkipSource.HeadsetGesture && !settingsCache.current().gesturesEnabled) return false
            if (audioManager.mode != AudioManager.MODE_NORMAL) return false
            if (!episodeState.casting && engine.player.playbackState != Player.STATE_READY) return false
            if (engine.player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                return false
            }
            return true
        }

        /** Ejecuta un salto adelante si [source] lo permite. */
        fun skipForward(
            source: SkipSource,
            gesture: String = "skip_forward",
        ) {
            dispatch(HeadsetCommand.SkipForward, source, gesture)
        }

        /** Ejecuta un salto atrás si [source] lo permite. */
        fun skipBack(
            source: SkipSource,
            gesture: String = "skip_back",
        ) {
            dispatch(HeadsetCommand.SkipBack, source, gesture)
        }

        /**
         * Gestiona una tecla multimedia física. Solo consume anterior/siguiente;
         * el resto (play/pausa) se delega en Media3.
         */
        fun onMediaButton(
            keyCode: Int,
            action: Int,
            repeatCount: Int,
        ): Boolean {
            if (keyCode != KeyEvent.KEYCODE_MEDIA_NEXT && keyCode != KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                return false
            }
            if (action != KeyEvent.ACTION_DOWN || repeatCount > 0) {
                // Repeticiones y ACTION_UP: consumir sin repetir el salto.
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                dispatch(HeadsetCommand.Next, SkipSource.HeadsetGesture, "media_next")
            } else {
                dispatch(HeadsetCommand.Previous, SkipSource.HeadsetGesture, "media_previous")
            }
            return true
        }

        /** Aplica [command] si [source] puede saltar y registra el resultado. */
        fun dispatch(
            command: HeadsetCommand,
            source: SkipSource,
            gesture: String,
        ) {
            val settings = settingsCache.current()
            val content = content()
            val allowed = canSkip(source)
            val action =
                if (allowed) {
                    HeadsetControlPolicy.decide(content, command, source, settings)
                } else {
                    HeadsetAction.Ignore
                }
            when (action) {
                is HeadsetAction.Skip -> {
                    seekBy(action.deltaMs)
                    log(gesture, content, source, "skip", action.deltaMs)
                }
                HeadsetAction.Ignore -> {
                    log(gesture, content, source, "ignore", 0L)
                }
            }
        }

        /** Deriva el contenido activo para la política. */
        private fun content(): PlaybackContent =
            when (val state = episodeController.state.value) {
                is EpisodePlaybackController.State.Active ->
                    if (state.playing) {
                        PlaybackContent.EpisodePlaying
                    } else {
                        PlaybackContent.EpisodePaused
                    }

                is EpisodePlaybackController.State.Loading,
                is EpisodePlaybackController.State.WaitingForNetwork,
                is EpisodePlaybackController.State.Reconnecting,
                is EpisodePlaybackController.State.Error,
                -> PlaybackContent.Idle

                EpisodePlaybackController.State.Idle ->
                    if (engine.player.mediaItemCount > 0) {
                        PlaybackContent.LiveRadio
                    } else {
                        PlaybackContent.Idle
                    }
            }

        /** Mueve la posición del episodio y la limita a [0, durationMs]. */
        private fun seekBy(deltaMs: Long) {
            val state = episodeController.state.value as? EpisodePlaybackController.State.Active ?: return
            val current = episodeController.positionMs()
            val target =
                if (state.durationMs > 0L) {
                    (current + deltaMs).coerceIn(0L, state.durationMs)
                } else {
                    (current + deltaMs).coerceAtLeast(0L)
                }
            episodeController.seekTo(target)
        }

        private fun log(
            gesture: String,
            content: PlaybackContent,
            source: SkipSource,
            result: String,
            deltaMs: Long,
        ) {
            // Sin PII ni datos del contenido; solo gesto, contenido y resultado.
            val contentLabel =
                when (content) {
                    PlaybackContent.EpisodePlaying -> "episode"
                    PlaybackContent.EpisodePaused -> "episode"
                    PlaybackContent.LiveRadio -> "radio"
                    PlaybackContent.Idle -> "idle"
                }
            val sourceLabel = if (source == SkipSource.HeadsetGesture) "headset" else "explicit"
            Log.d(
                TAG,
                "gesture=$gesture content=$contentLabel source=$sourceLabel " +
                    "result=$result deltaMs=$deltaMs",
            )
        }

        private companion object {
            const val TAG = "HeadsetControls"
        }
    }
