package com.izquierdojl.tolocharadio.domain.podcasts

/**
 * Estado de la precarga del episodio en curso (spec 0058, FR-014/FR-015).
 *
 * [targetBytes] `null` significa "hasta el final del episodio" (red no medida).
 */
data class PrefetchState(
    val episodeId: Long? = null,
    val cachedBytes: Long = 0,
    val targetBytes: Long? = null,
    val running: Boolean = false,
) {
    init {
        require(episodeId == null || episodeId > 0) { "episodeId debe ser > 0 o null" }
        require(cachedBytes >= 0) { "cachedBytes debe ser >= 0" }
        require(targetBytes == null || targetBytes >= 0) { "targetBytes debe ser >= 0 o null" }
    }

    companion object {
        val IDLE: PrefetchState = PrefetchState()
    }
}

/**
 * Convierte la política de red en un objetivo de precarga (spec 0058, R6).
 *
 * En redes no medidas el objetivo es el episodio completo; en medidas, 5
 * minutos por delante estimados a [bitrateBps] (128 kbps por defecto).
 */
data class EpisodePrefetchPolicy(
    val cachePolicy: EpisodeCachePolicy = EpisodeCachePolicy.DEFAULT,
    val bitrateBps: Long = DEFAULT_BITRATE_BPS,
) {
    init {
        require(bitrateBps > 0) { "bitrateBps debe ser > 0" }
    }

    /** Bytes a precargar; `null` = hasta el final. */
    fun targetBytes(kind: NetworkKind): Long? =
        when (kind) {
            NetworkKind.UNMETERED -> null
            NetworkKind.METERED ->
                ((cachePolicy.prefetchAheadMeteredMs / MS_PER_SEC) * bitrateBps / BITS_PER_BYTE)
                    .coerceAtMost(cachePolicy.limitMeteredBytes)
        }

    companion object {
        /** Bitrate medio de los podcasts servidos por el proxy. */
        const val DEFAULT_BITRATE_BPS: Long = 128_000L

        private const val MS_PER_SEC = 1_000L
        private const val BITS_PER_BYTE = 8L

        val DEFAULT: EpisodePrefetchPolicy = EpisodePrefetchPolicy()
    }
}
