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
import com.izquierdojl.tolocharadio.domain.playback.ContentKind
import com.izquierdojl.tolocharadio.domain.playback.EpisodeBufferSpec
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
        fun create(profile: BufferProfile): ExoPlayer = create(profile, ContentKind.LIVE)

        /**
         * Crea el player del tipo de contenido: [ContentKind.LIVE] usa el perfil
         * de radio del usuario y [ContentKind.VOD] el buffer profundo de
         * episodios (spec 0058, FR-001/FR-002).
         */
        @OptIn(UnstableApi::class)
        fun create(
            profile: BufferProfile,
            kind: ContentKind,
        ): ExoPlayer {
            // Una instancia por player: al cambiar de perfil no se reutiliza
            // un procesador ya configurado por el sink anterior.
            val processor = VoiceBoostAudioProcessor { voiceBoost.isEffective() }
            val renderers = VoiceBoostRenderersFactory(context, processor)
            val builder = ExoPlayer.Builder(context).setRenderersFactory(renderers)
            val configured =
                when (kind) {
                    ContentKind.LIVE ->
                        PlayerAudioConfig.applyTo(builder, BufferProfileSpec.forProfile(profile))
                    ContentKind.VOD ->
                        PlayerAudioConfig.applyTo(builder, EpisodeBufferSpec.DEFAULT)
                }
            return configured
                .setSeekBackIncrementMs(EPISODE_SKIP_BACK_MS)
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
