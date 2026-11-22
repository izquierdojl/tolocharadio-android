package com.izquierdojl.tolocharadio.domain.podcasts

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Caché de audio de episodios (spec 0058, contrato `episode-cache.md`).
 *
 * La implementación vive en `data/local/podcasts`; el dominio solo conoce los
 * límites, las estadísticas y la factoría de datasources.
 */
interface EpisodeCache {
    /** Espacio ocupado y número de entradas. */
    val stats: Flow<EpisodeCacheStats>

    /** Datasource de lectura/escritura para un episodio en streaming. */
    fun factoryFor(episodeId: Long): DataSource.Factory

    /** Datasource sin caché para descargas locales o Cast. */
    fun passthrough(): DataSource.Factory

    /** Datasource de caché para la precarga; `null` si la caché no está lista. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun cacheDataSource(episodeId: Long): CacheDataSource?

    /** Ajusta el límite al tipo de red activo. */
    fun setNetworkKind(kind: NetworkKind)

    /** Vacía la caché (Ajustes, FR-012). */
    suspend fun clear()

    companion object {
        const val KEY_PREFIX: String = "episode-"

        /** Clave estable: el episodio, nunca la URL firmada (FR-007). */
        fun cacheKey(episodeId: Long): String = "$KEY_PREFIX$episodeId"
    }
}
