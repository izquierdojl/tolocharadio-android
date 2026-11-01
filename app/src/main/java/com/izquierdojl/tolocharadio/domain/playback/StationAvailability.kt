package com.izquierdojl.tolocharadio.domain.playback

/**
 * Resultado del precheck de disponibilidad de una emisora (`/playback/{id}/status`).
 *
 * [checkedAt] permite reutilizar el resultado durante [DEFAULT_TTL_MS] y evitar
 * una segunda consulta al abrir la ficha y pulsar play.
 */
data class StationAvailability(
    val stationId: String,
    val playable: Boolean,
    val reason: String?,
    val checkedAt: Long,
) {
    /** `true` si el resultado sigue vigente para [nowMs]. */
    fun isFresh(nowMs: Long): Boolean = nowMs - checkedAt in 0..DEFAULT_TTL_MS

    companion object {
        /** Ventana breve para compartir el precheck entre ficha y play. */
        const val DEFAULT_TTL_MS = 5_000L
    }
}
