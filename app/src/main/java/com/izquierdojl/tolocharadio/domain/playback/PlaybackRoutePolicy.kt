package com.izquierdojl.tolocharadio.domain.playback

/** Decisión de cambiar una sola vez de la ruta continua a la ruta de reserva. */
sealed interface FallbackDecision {
    data class UseLegacy(
        val maxAttempts: Int,
    ) : FallbackDecision

    data object None : FallbackDecision
}

/**
 * Política pura de fallback (spec 0044, FR-003/FR-004).
 *
 * 415 usa un único intento legacy; 404/405 usan la política normal. Una vez
 * intentado el fallback no se vuelve a cambiar de ruta.
 */
object PlaybackRoutePolicy {
    fun decide(
        route: PlaybackRoute,
        cause: InterruptionCause,
        fallbackAttempted: Boolean,
        policy: RecoveryPolicy,
    ): FallbackDecision {
        if (route == PlaybackRoute.LEGACY || fallbackAttempted) return FallbackDecision.None
        return when (cause) {
            InterruptionCause.UNSUPPORTED_FORMAT -> FallbackDecision.UseLegacy(maxAttempts = 1)
            InterruptionCause.LIVE_ROUTE_UNAVAILABLE ->
                FallbackDecision.UseLegacy(maxAttempts = policy.maxAttempts)
            else -> FallbackDecision.None
        }
    }
}
