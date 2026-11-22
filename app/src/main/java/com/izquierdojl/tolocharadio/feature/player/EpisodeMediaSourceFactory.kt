package com.izquierdojl.tolocharadio.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Construye el `MediaSource` de un episodio con caché en disco
 * (spec 0058, R4/R5).
 *
 * La clave es `episode-<id>`, nunca la URL firmada, para que renovar la firma
 * no invalide el audio guardado. Los archivos locales y Cast no usan caché.
 */
@Singleton
class EpisodeMediaSourceFactory
    @Inject
    constructor(
        private val cache: EpisodeCache,
    ) {
        @OptIn(UnstableApi::class)
        fun create(
            item: MediaItem,
            episodeId: Long,
            local: Boolean,
        ): MediaSource {
            val keyed =
                if (local) {
                    item
                } else {
                    item.buildUpon().setCustomCacheKey(keyFor(episodeId)).build()
                }
            return ProgressiveMediaSource.Factory(dataSourceFor(episodeId, local)).createMediaSource(keyed)
        }

        /** Clave estable de la caché (FR-007). */
        internal fun keyFor(episodeId: Long): String = EpisodeCache.cacheKey(episodeId)

        /** `factoryFor` para streaming; `passthrough` para archivos locales (FR-009). */
        internal fun dataSourceFor(
            episodeId: Long,
            local: Boolean,
        ): DataSource.Factory = if (local) cache.passthrough() else cache.factoryFor(episodeId)
    }
