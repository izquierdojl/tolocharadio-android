package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Envoltorio Media3 del DSP: no debe perder el búfer al procesar (US4). */
@OptIn(UnstableApi::class)
class VoiceBoostAudioProcessorTest {
    private fun samples(count: Int): ShortArray = ShortArray(count) { index -> (index % 100).toShort() }

    private fun input(samples: ShortArray): ByteBuffer =
        ByteBuffer
            .allocateDirect(samples.size * 2)
            .order(ByteOrder.nativeOrder())
            .apply {
                asShortBuffer().put(samples)
                position(0)
                limit(samples.size * 2)
            }

    private fun configured(): AudioProcessor.AudioFormat = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)

    @Test
    fun `activado entrega un buffer de salida con audio`() {
        val processor = VoiceBoostAudioProcessor { true }
        processor.configure(configured())
        val format = configured()
        val input = input(samples(2_048))

        processor.queueInput(input)

        assertEquals(format.sampleRate, 48_000)
        assertEquals(4_096, processor.output.remaining())
    }

    @Test
    fun `desactivado copia el buffer sin procesarlo`() {
        val processor = VoiceBoostAudioProcessor { false }
        processor.configure(configured())
        val input = input(samples(2_048))

        processor.queueInput(input)

        assertEquals(4_096, processor.output.remaining())
    }
}
