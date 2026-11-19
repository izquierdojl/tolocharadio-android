package com.izquierdojl.tolocharadio.feature.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.playback.BufferProfileSpec
import com.izquierdojl.tolocharadio.domain.podcasts.EPISODE_SKIP_BACK_MS
import com.izquierdojl.tolocharadio.domain.podcasts.EPISODE_SKIP_FORWARD_MS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fábrica de `ExoPlayer` con el `LoadControl` de un perfil (spec 0042, R2),
 * los incrementos de salto de episodio y el procesador de voz (spec 0047).
 */
@Singleton
class PlaybackPlayerFactory
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val voiceBoost: VoiceBoostController,
    ) {
        @OptIn(UnstableApi::class)
        fun create(profile: BufferProfile): ExoPlayer {
            // Una instancia por player: al cambiar de perfil no se reutiliza
            // un procesador ya configurado por el sink anterior.
            val processor = VoiceBoostAudioProcessor { voiceBoost.isEffective() }
            val renderers = VoiceBoostRenderersFactory(context, processor)
            return PlayerAudioConfig
                .applyTo(
                    ExoPlayer.Builder(context).setRenderersFactory(renderers),
                    BufferProfileSpec.forProfile(profile),
                ).setSeekBackIncrementMs(EPISODE_SKIP_BACK_MS)
                .setSeekForwardIncrementMs(EPISODE_SKIP_FORWARD_MS)
                .build()
        }
    }

/** Renderers factory que construye un `DefaultAudioSink` con el DSP de voz. */
@OptIn(UnstableApi::class)
private class VoiceBoostRenderersFactory(
    context: Context,
    private val processor: VoiceBoostAudioProcessor,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParameters: Boolean,
    ): AudioSink =
        DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(processor))
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParameters)
            .build()
}
