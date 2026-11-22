package com.izquierdojl.tolocharadio.feature.player

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * Compresor de rango dinámico + ganancia de compensación + limitador
 * suave para el potenciador de voz (spec 0047, US4).
 *
 * Es Kotlin puro (sin Android) para poder probarlo en JVM. El búfer es
 * PCM 16 bits entrelazado (mono o estéreo) y se procesa in situ.
 */
class VoiceBoostDsp(
    private val sampleRate: Int,
    private val channels: Int,
) {
    private var envelope = 0.0
    private var gain = 1.0
    private var initialized = false

    private val attackCoef = 1.0 - exp(-1.0 / (sampleRate * ATTACK_SECONDS))
    private val releaseCoef = 1.0 - exp(-1.0 / (sampleRate * RELEASE_SECONDS))
    private val gainCoef = 1.0 - exp(-1.0 / (sampleRate * GAIN_SMOOTH_SECONDS))
    private val threshold = dbToLinear(THRESHOLD_DB)
    private val makeup = dbToLinear(MAKEUP_DB)

    init {
        require(sampleRate > 0) { "sampleRate debe ser positivo" }
        require(channels in MIN_CHANNELS..MAX_CHANNELS) { "canales no soportados" }
    }

    /** Procesa [length] muestras del búfer; el resto no se toca. */
    fun process(
        buffer: ShortArray,
        length: Int,
    ) {
        val frames = (length / channels).coerceAtMost(buffer.size / channels)
        for (frame in 0 until frames) {
            var peak = 0.0
            val base = frame * channels
            for (channel in 0 until channels) {
                peak = max(peak, abs(buffer[base + channel].toDouble()) / FULL_SCALE)
            }
            val coefficient = if (peak > envelope) attackCoef else releaseCoef
            envelope += coefficient * (peak - envelope)

            val target = targetGain(envelope)
            if (!initialized) {
                gain = target
                initialized = true
            } else {
                gain += gainCoef * (target - gain)
            }

            for (channel in 0 until channels) {
                val index = base + channel
                val normalized = buffer[index] / FULL_SCALE * gain
                val limited = softLimit(normalized).coerceIn(-OUTPUT_CEILING, OUTPUT_CEILING)
                buffer[index] =
                    (limited * FULL_SCALE)
                        .roundToInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
            }
        }
    }

    /** Reinicia la envolvente y la ganancia (cambio de pista o flush). */
    fun reset() {
        envelope = 0.0
        gain = 1.0
        initialized = false
    }

    private fun targetGain(envelopeLinear: Double): Double {
        if (envelopeLinear <= threshold) return makeup
        val envelopeDb = DB_MULTIPLIER * log10(envelopeLinear)
        val compressedDb = THRESHOLD_DB + (envelopeDb - THRESHOLD_DB) / RATIO
        val gainDb = compressedDb - envelopeDb + MAKEUP_DB
        return dbToLinear(gainDb)
    }

    private fun softLimit(sample: Double): Double {
        val magnitude = abs(sample)
        if (magnitude <= LIMIT_THRESHOLD) return sample
        val sign = if (sample < 0.0) -1.0 else 1.0
        val limited =
            LIMIT_THRESHOLD +
                (1.0 - LIMIT_THRESHOLD) *
                tanh((magnitude - LIMIT_THRESHOLD) / (1.0 - LIMIT_THRESHOLD))
        return sign * limited
    }

    private fun dbToLinear(db: Double): Double = 10.0.pow(db / DB_MULTIPLIER)

    private companion object {
        const val MIN_CHANNELS = 1
        const val MAX_CHANNELS = 2
        const val ATTACK_SECONDS = 0.001
        const val RELEASE_SECONDS = 0.150
        const val GAIN_SMOOTH_SECONDS = 0.005
        const val THRESHOLD_DB = -24.0
        const val RATIO = 3.0
        const val MAKEUP_DB = 10.0
        const val LIMIT_THRESHOLD = 0.95
        const val OUTPUT_CEILING = 0.99
        const val FULL_SCALE = 32_768.0
        const val DB_MULTIPLIER = 20.0
    }
}
