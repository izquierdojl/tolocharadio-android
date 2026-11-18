package com.izquierdojl.tolocharadio.domain.podcasts

/** Estado de escucha derivado del progreso (filtros de ficha, FR-012). */
enum class EpisodeProgressState {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
}

/** El completado del servidor manda sobre la posición guardada. */
fun progressState(
    positionSec: Long,
    completed: Boolean,
): EpisodeProgressState =
    when {
        completed -> EpisodeProgressState.COMPLETED
        positionSec > 0 -> EpisodeProgressState.IN_PROGRESS
        else -> EpisodeProgressState.NOT_STARTED
    }
