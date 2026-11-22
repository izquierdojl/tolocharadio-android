package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.domain.playback.BufferProfileSpec
import com.izquierdojl.tolocharadio.domain.playback.EpisodeBufferSpec

/**
 * Configuración de audio del reproductor local (spec 0019, FR-001..FR-005).
 *
 * - `USAGE_MEDIA` hace que Media3 solicite `AUDIOFOCUS_GAIN`, de modo que otras
 *   apps que respetan el foco se detienen al reproducir TolochaRadio.
 * - `CONTENT_TYPE_SPEECH` es la condición que Media3 usa para
 *   `willPauseWhenDucked()`: ante `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` **pausa**
 *   en lugar de bajar el volumen (decisión de clarify: sin duck).
 * - `handleAudioBecomingNoisy` pausa al desconectar auriculares/Bluetooth.
 */
@OptIn(UnstableApi::class)
object PlayerAudioConfig {
    /** Media3 gestiona el foco automáticamente para este player. */
    const val HANDLE_AUDIO_FOCUS: Boolean = true

    /** Pausa al desconectarse auriculares/Bluetooth. */
    const val HANDLE_AUDIO_BECOMING_NOISY: Boolean = true

    /** Atributos con semántica de medios y contenido hablado (pausa en vez de duck). */
    val audioAttributes: AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

    /** Aplica foco de audio y comportamiento "becoming noisy" al builder. */
    fun applyTo(builder: ExoPlayer.Builder): ExoPlayer.Builder =
        builder
            .setAudioAttributes(audioAttributes, HANDLE_AUDIO_FOCUS)
            .setHandleAudioBecomingNoisy(HANDLE_AUDIO_BECOMING_NOISY)

    /** Construye el `LoadControl` del perfil (spec 0042, R1). */
    fun loadControlFor(spec: BufferProfileSpec): DefaultLoadControl =
        buildLoadControl(
            spec.minBufferMs,
            spec.maxBufferMs,
            spec.bufferForPlaybackMs,
            spec.bufferForPlaybackAfterRebufferMs,
        )

    /** Construye el `LoadControl` profundo de episodios (spec 0058, FR-002). */
    fun loadControlFor(spec: EpisodeBufferSpec): DefaultLoadControl =
        buildLoadControl(
            spec.minBufferMs,
            spec.maxBufferMs,
            spec.bufferForPlaybackMs,
            spec.bufferForPlaybackAfterRebufferMs,
        )

    private fun buildLoadControl(
        minBufferMs: Long,
        maxBufferMs: Long,
        bufferForPlaybackMs: Long,
        bufferForPlaybackAfterRebufferMs: Long,
    ): DefaultLoadControl =
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                minBufferMs.toInt(),
                maxBufferMs.toInt(),
                bufferForPlaybackMs.toInt(),
                bufferForPlaybackAfterRebufferMs.toInt(),
            ).build()

    /** Aplica atributos de audio y el buffer del perfil. */
    fun applyTo(
        builder: ExoPlayer.Builder,
        spec: BufferProfileSpec,
    ): ExoPlayer.Builder = applyTo(builder).setLoadControl(loadControlFor(spec))

    /** Aplica atributos de audio y el buffer profundo de episodios. */
    fun applyTo(
        builder: ExoPlayer.Builder,
        spec: EpisodeBufferSpec,
    ): ExoPlayer.Builder = applyTo(builder).setLoadControl(loadControlFor(spec))
}
