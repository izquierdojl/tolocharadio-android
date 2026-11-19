package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress

/** Milisegundos por segundo. */
private const val MS_PER_SEC = 1_000L

/** Origen del progreso usado para reanudar (diagnóstico y tests). */
enum class ResumeSource {
    /** No había progreso válido. */
    NONE,

    /** El episodio traía el progreso embebido en la lista. */
    EMBEDDED,

    /** Se consultó al servidor porque la lista no lo traía. */
    REMOTE,
}

/** Punto de reanudación resuelto para un episodio. */
data class ResumePoint(
    val episodeId: Long,
    val positionMs: Long,
    val source: ResumeSource,
) {
    val positionSec: Long get() = positionMs / MS_PER_SEC
}

/**
 * Regla pura de reanudación (FR-001–FR-003):
 * sin progreso, completado, posición no positiva o fuera de rango → 0.
 * En caso contrario, la posición guardada.
 */
fun resumePositionSec(
    progress: PodcastEpisodeProgress?,
    durationSec: Long?,
): Long {
    if (progress == null || progress.completed) return 0
    val position = progress.positionSec
    if (position <= 0) return 0
    if (durationSec != null && durationSec > 0 && position >= durationSec) return 0
    return position
}

/** Construye el [ResumePoint] con la regla de reanudación y su origen. */
fun resumePoint(
    episodeId: Long,
    progress: PodcastEpisodeProgress?,
    durationSec: Long?,
    source: ResumeSource,
): ResumePoint {
    val resolvedSec = resumePositionSec(progress, durationSec)
    return ResumePoint(
        episodeId = episodeId,
        positionMs = resolvedSec * MS_PER_SEC,
        source = if (resolvedSec > 0) source else ResumeSource.NONE,
    )
}
