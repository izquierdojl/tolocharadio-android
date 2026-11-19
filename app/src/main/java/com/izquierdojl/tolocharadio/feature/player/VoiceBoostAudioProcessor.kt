package com.izquierdojl.tolocharadio.feature.player

import android.annotation.SuppressLint
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Envoltorio Media3 del DSP de voz (spec 0047, US4). Acepta PCM 16 bits
 * mono/estéreo y hace bypass copiando el búfer cuando la política está
 * desactivada, para poder alternarlo sin recrear el reproductor.
 */
@UnstableApi
class VoiceBoostAudioProcessor(
    private val isEnabled: () -> Boolean,
) : BaseAudioProcessor() {
    private var dsp: VoiceBoostDsp? = null

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.channelCount !in 1..2
        ) {
            dsp = null
            return AudioProcessor.AudioFormat.NOT_SET
        }
        dsp = VoiceBoostDsp(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining < 2) {
            inputBuffer.position(inputBuffer.limit())
            return
        }
        val output = replaceOutputBuffer(remaining)
        val dsp = dsp
        if (!isEnabled() || dsp == null) {
            output.put(inputBuffer)
        } else {
            val samples = ShortArray(remaining / 2)
            inputBuffer.asShortBuffer().get(samples)
            inputBuffer.position(inputBuffer.limit())
            dsp.process(samples, samples.size)
            output.asShortBuffer().put(samples)
            // La vista ShortBuffer tiene su propia posición: hay que avanzar
            // el ByteBuffer padre antes de flip o el sink recibe un búfer vacío.
            output.position(remaining)
        }
        output.flip()
    }

    @SuppressLint("MissingSuperCall")
    override fun isActive(): Boolean = true

    override fun onFlush() {
        dsp?.reset()
    }

    override fun onReset() {
        dsp?.reset()
    }
}
