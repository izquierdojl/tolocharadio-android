package com.izquierdojl.tolocharadio.domain.playback

/** Ruta efectiva de reproducción (spec 0044; VOD añadido en spec 0046). */
enum class PlaybackRoute {
    /** Flujo continuo `/playback/{id}/live`, reproducido como audio progresivo. */
    CONTINUOUS,

    /** Ruta anterior `/playback/{id}` con manifiesto HLS o audio progresivo. */
    LEGACY,

    /** Episodio de podcast: fichero finito vía URL firmada (sin reconexión live). */
    EPISODE,
}
