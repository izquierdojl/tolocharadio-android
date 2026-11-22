package com.izquierdojl.tolocharadio.domain.podcasts

/** Clasificación de red para la política de caché (spec 0058, R7). */
enum class NetworkKind {
    UNMETERED,
    METERED,
}

/**
 * Límites de la caché de episodios (spec 0058, FR-008/FR-014/FR-015).
 *
 * 500 MB en redes no medidas y 50 MB en medidas; en móvil se precargan como
 * máximo 5 minutos por delante. Los valores están en bytes.
 */
data class EpisodeCachePolicy(
    val limitUnmeteredBytes: Long = LIMIT_UNMETERED_BYTES,
    val limitMeteredBytes: Long = LIMIT_METERED_BYTES,
    val prefetchAheadMeteredMs: Long = PREFETCH_AHEAD_METERED_MS,
) {
    init {
        require(limitUnmeteredBytes > 0) { "limitUnmeteredBytes debe ser > 0" }
        require(limitMeteredBytes > 0) { "limitMeteredBytes debe ser > 0" }
        require(limitUnmeteredBytes >= limitMeteredBytes) {
            "limitUnmeteredBytes debe ser >= limitMeteredBytes"
        }
        require(prefetchAheadMeteredMs in 1..PREFETCH_AHEAD_MAX_MS) {
            "prefetchAheadMeteredMs debe estar entre 1 y $PREFETCH_AHEAD_MAX_MS"
        }
    }

    /** Límite aplicable a la red actual. */
    fun limitFor(kind: NetworkKind): Long =
        when (kind) {
            NetworkKind.UNMETERED -> limitUnmeteredBytes
            NetworkKind.METERED -> limitMeteredBytes
        }

    companion object {
        /** 500 MiB. */
        const val LIMIT_UNMETERED_BYTES: Long = 524_288_000L

        /** 50 MiB. */
        const val LIMIT_METERED_BYTES: Long = 52_428_800L

        /** 5 minutos. */
        const val PREFETCH_AHEAD_METERED_MS: Long = 300_000L

        private const val PREFETCH_AHEAD_MAX_MS = 300_000L

        val DEFAULT: EpisodeCachePolicy = EpisodeCachePolicy()
    }
}

/** Resumen de la caché para Ajustes (spec 0058, FR-012). */
data class EpisodeCacheStats(
    val sizeBytes: Long,
    val entryCount: Int,
) {
    init {
        require(sizeBytes >= 0) { "sizeBytes debe ser >= 0" }
        require(entryCount >= 0) { "entryCount debe ser >= 0" }
    }

    companion object {
        val EMPTY: EpisodeCacheStats = EpisodeCacheStats(sizeBytes = 0, entryCount = 0)
    }
}
