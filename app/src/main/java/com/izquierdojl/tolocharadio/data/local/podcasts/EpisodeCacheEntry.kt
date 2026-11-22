package com.izquierdojl.tolocharadio.data.local.podcasts

/**
 * Entrada de audio de un episodio en la caché local (spec 0058, data-model).
 *
 * [episodeId] es la identidad, nunca la URL firmada; [bytes] es lo cacheado y
 * [lastUsedAt] el instante de último uso para el desalojo LRU.
 */
data class EpisodeCacheEntry(
    val episodeId: Long,
    val bytes: Long,
    val lastUsedAt: Long,
) {
    init {
        require(episodeId > 0) { "episodeId debe ser > 0" }
        require(bytes >= 0) { "bytes debe ser >= 0" }
        require(lastUsedAt >= 0) { "lastUsedAt debe ser >= 0" }
    }
}
