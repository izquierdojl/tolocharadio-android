package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

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
}
