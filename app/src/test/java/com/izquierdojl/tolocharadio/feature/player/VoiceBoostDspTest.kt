package com.izquierdojl.tolocharadio.feature.player

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/** DSP del potenciador de voz (US4, FR-025/FR-028). */
class VoiceBoostDspTest {
    private val sampleRate = 48_000

    private fun sine(
        amplitude: Double,
        frames: Int,
        channels: Int = 1,
    ): ShortArray {
        val buffer = ShortArray(frames * channels)
        for (frame in 0 until frames) {
            val sample = (sin(2.0 * PI * 440.0 * frame / sampleRate) * amplitude * Short.MAX_VALUE).toInt().toShort()
            for (channel in 0 until channels) {
                buffer[frame * channels + channel] = sample
            }
        }
        return buffer
    }

    private fun rms(
        buffer: ShortArray,
        from: Int,
        to: Int,
    ): Double {
        var sum = 0.0
        for (i in from until to) {
            val value = buffer[i].toDouble() / Short.MAX_VALUE
            sum += value * value
        }
        return sqrt(sum / (to - from))
    }

    @Test
    fun `una voz baja gana nivel`() {
        val input = sine(amplitude = 0.05, frames = 8_192)
        val output = input.copyOf()
        VoiceBoostDsp(sampleRate, channels = 1).process(output, output.size)

        assertTrue(rms(output, 4_096, output.size) > rms(input, 4_096, input.size))
    }

    @Test
    fun `una voz a nivel de podcast gana al menos cinco decibelios`() {
        // Pico a -18 dBFS, el nivel habitual de una voz ya normalizada.
        val input = sine(amplitude = 0.125, frames = 48_000)
        val output = input.copyOf()
        VoiceBoostDsp(sampleRate, channels = 1).process(output, output.size)

        val gainDb = 20.0 * log10(rms(output, 24_000, output.size) / rms(input, 24_000, input.size))

        assertTrue("ganancia insuficiente: $gainDb dB", gainDb >= 5.5)
    }

    @Test
    fun `una senal alta no llega a saturar`() {
        val output = sine(amplitude = 0.99, frames = 8_192)
        VoiceBoostDsp(sampleRate, channels = 1).process(output, output.size)

        assertTrue(output.all { it != Short.MAX_VALUE && it != Short.MIN_VALUE })
    }

    @Test
    fun `el estereo procesa ambos canales igual`() {
        val output = sine(amplitude = 0.08, frames = 2_048, channels = 2)
        VoiceBoostDsp(sampleRate, channels = 2).process(output, output.size)

        for (frame in 0 until output.size / 2) {
            assertEquals(output[frame * 2], output[frame * 2 + 1])
        }
    }

    @Test
    fun `reset devuelve el estado inicial`() {
        val dsp = VoiceBoostDsp(sampleRate, channels = 1)
        val loud = sine(amplitude = 0.9, frames = 4_096)
        dsp.process(loud, loud.size)
        dsp.reset()

        val afterReset = sine(amplitude = 0.05, frames = 4_096)
        val fresh = afterReset.copyOf()
        dsp.process(afterReset, afterReset.size)
        VoiceBoostDsp(sampleRate, channels = 1).process(fresh, fresh.size)

        assertArrayEquals(fresh, afterReset)
    }

    @Test
    fun `un buffer vacio no altera el estado`() {
        val dsp = VoiceBoostDsp(sampleRate, channels = 1)
        val empty = ShortArray(0)
        dsp.process(empty, 0)
        assertEquals(0, empty.size)
    }

    @Test
    fun `un pico aislado no desborda el rango`() {
        val buffer = ShortArray(2)
        buffer[0] = Short.MAX_VALUE
        buffer[1] = Short.MIN_VALUE
        VoiceBoostDsp(sampleRate, channels = 1).process(buffer, buffer.size)

        assertTrue(buffer.none { it == Short.MAX_VALUE || it == Short.MIN_VALUE })
    }
}
