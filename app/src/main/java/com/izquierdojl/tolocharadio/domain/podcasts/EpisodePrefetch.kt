package com.izquierdojl.tolocharadio.domain.podcasts

import kotlinx.coroutines.flow.StateFlow

/**
 * Precarga del episodio en curso (spec 0058, contrato `episode-cache.md`).
 *
 * La implementación con `CacheWriter` vive en `data/local/podcasts`.
 */
interface EpisodePrefetch {
    val state: StateFlow<PrefetchState>

    /** Arranca o reinicia la precarga desde [positionMs]. */
    fun start(
        source: EpisodePlaybackSource,
        episodeId: Long,
        positionMs: Long,
    )

    /** Avanza el punto de referencia; en red medida puede reiniciar el margen. */
    fun update(positionMs: Long)

    /** Cancela la precarga en curso. */
    fun stop()
}
