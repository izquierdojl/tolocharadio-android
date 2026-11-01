package com.izquierdojl.tolocharadio.domain.playback

/** Ruta efectiva de reproducción de una emisora (spec 0044). */
enum class PlaybackRoute {
    /** Flujo continuo `/playback/{id}/live`, reproducido como audio progresivo. */
    CONTINUOUS,

    /** Ruta anterior `/playback/{id}` con manifiesto HLS o audio progresivo. */
    LEGACY,
}
