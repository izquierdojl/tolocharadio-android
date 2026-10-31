package com.izquierdojl.tolocharadio.domain.playback

/**
 * Política de reintentos de recuperación (spec 0042, FR-003/FR-015).
 *
 * Los valores son de producto y deben coincidir con `data-model.md` y `research.md`.
 */
@Suppress("MagicNumber")
data class RecoveryPolicy(
    val maxAttempts: Int = 5,
    val totalTimeoutMs: Long = 60_000,
    val attemptTimeoutMs: Long = 7_000,
    val stallThresholdMs: Long = 5_000,
    val initialBufferTimeoutMs: Long = 10_000,
    val backoffMs: List<Long> = listOf(0, 1_000, 2_000, 4_000, 8_000),
) {
    init {
        require(maxAttempts in 1..5) { "maxAttempts debe estar entre 1 y 5" }
        require(totalTimeoutMs in 1..60_000) { "totalTimeoutMs no debe superar 60 s" }
        require(attemptTimeoutMs in 1..7_000) { "attemptTimeoutMs no debe superar 7 s" }
        require(stallThresholdMs == 5_000L) { "stallThresholdMs debe ser 5 s" }
        require(initialBufferTimeoutMs == 10_000L) { "initialBufferTimeoutMs debe ser 10 s" }
        require(backoffMs.isNotEmpty()) { "backoffMs no puede estar vacío" }
        require(backoffMs.all { it >= 0 }) { "backoffMs no puede contener negativos" }
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
