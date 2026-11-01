package com.izquierdojl.tolocharadio.domain.playback

/**
 * Política de reintentos y watchdog de la reproducción (spec 0044, FR-007).
 *
 * El presupuesto de intentos se comparte dentro de [stableWindowMs]; cada
 * secuencia de reconexión tiene su propio límite [totalTimeoutMs].
 */
@Suppress("MagicNumber")
data class RecoveryPolicy(
    val maxAttempts: Int = 6,
    val totalTimeoutMs: Long = 120_000,
    val attemptTimeoutMs: Long = 15_000,
    val stallThresholdMs: Long = 15_000,
    val initialBufferTimeoutMs: Long = 15_000,
    val backoffMs: List<Long> = listOf(0, 1_000, 2_000, 4_000, 8_000, 15_000),
    val stableWindowMs: Long = 60_000,
) {
    init {
        require(maxAttempts in 1..6) { "maxAttempts debe estar entre 1 y 6" }
        require(totalTimeoutMs in 1..120_000) { "totalTimeoutMs no debe superar 120 s" }
        require(attemptTimeoutMs in 1..15_000) { "attemptTimeoutMs no debe superar 15 s" }
        require(stallThresholdMs == 15_000L) { "stallThresholdMs debe ser 15 s" }
        require(initialBufferTimeoutMs == 15_000L) { "initialBufferTimeoutMs debe ser 15 s" }
        require(backoffMs.isNotEmpty()) { "backoffMs no puede estar vacío" }
        require(backoffMs.all { it >= 0 }) { "backoffMs no puede contener negativos" }
        require(stableWindowMs == 60_000L) { "stableWindowMs debe ser 60 s" }
    }

    /** Espera antes del intento [attempt] (1-based). */
    fun delayForAttempt(attempt: Int): Long {
        if (attempt <= 0) return 0
        return backoffMs.getOrElse(attempt - 1) { backoffMs.last() }
    }

    /** Hay presupuesto para otro intento dentro de la ventana total. */
    fun canAttempt(
        attempt: Int,
        elapsedMs: Long,
    ): Boolean = attempt < maxAttempts && elapsedMs < totalTimeoutMs
}
