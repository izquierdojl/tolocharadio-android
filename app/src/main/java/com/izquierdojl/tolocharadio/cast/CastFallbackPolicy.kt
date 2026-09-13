package com.izquierdojl.tolocharadio.cast

/** Acción de recuperación tras terminar una sesión Cast (bugs 0031/0032). */
enum class CastFallbackDecision {
    /** El receptor quedó detenido de forma segura: reanudar en local sonando. */
    RESUME_LOCAL,

    /** El receptor quedó detenido y estaba en pausa: preparar local en pausa. */
    RESUME_LOCAL_PAUSED,

    /** El receptor puede seguir reproduciendo: no arrancar local, avisar. */
    DO_NOT_RESUME_LOCAL,
}

/**
 * Decide qué hacer con la reproducción local al terminar una sesión Cast
 * sin duplicar audio (spec 011, SC-005).
 *
 * Solo es seguro reanudar en local cuando la propia app o el usuario pararon
 * el receptor ([userInitiated]); ante una pérdida de conexión no intencionada
 * el receptor autónomo puede seguir sonando, así que nunca se arranca local.
 */
object CastFallbackPolicy {
    fun decide(
        userInitiated: Boolean,
        remoteWasPlaying: Boolean,
    ): CastFallbackDecision =
        when {
            !userInitiated -> CastFallbackDecision.DO_NOT_RESUME_LOCAL
            remoteWasPlaying -> CastFallbackDecision.RESUME_LOCAL
            else -> CastFallbackDecision.RESUME_LOCAL_PAUSED
        }
}
