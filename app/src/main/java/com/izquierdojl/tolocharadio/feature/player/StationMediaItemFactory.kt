package com.izquierdojl.tolocharadio.feature.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.izquierdojl.tolocharadio.data.remote.api.streamUrl
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Único punto de construcción de `MediaItem`/`MediaSource` para reproducción
 * local, Cast y reanudación post-Cast (spec 0019, research R7). Evita que las
 * tres rutas diverjan al añadir HLS y mime types.
 */
@OptIn(UnstableApi::class)
@Singleton
class StationMediaItemFactory
    @Inject
    constructor(
        private val authDataSource: AuthDataSourceFactory,
        private val directDataSource: DirectDataSourceFactory,
    ) {
        /** URL efectiva de la fuente: proxy para [PlaybackSource.Proxied], directa si no. */
        fun uriFor(
            source: PlaybackSource,
            baseUrl: String,
        ): String =
            when (source) {
                is PlaybackSource.Proxied -> streamUrl(baseUrl, source.stationId)
                is PlaybackSource.DirectProgressive -> source.url
                is PlaybackSource.DirectHls -> source.url
            }

        /** `mimeType` del `MediaItem`; HLS para [PlaybackSource.DirectHls], ninguno en el resto. */
        fun mimeTypeFor(source: PlaybackSource): String? {
            return if (source is PlaybackSource.DirectHls) MimeTypes.APPLICATION_M3U8 else null
        }

        /** Construye el `MediaItem` con metadata y `mimeType` HLS cuando aplica. */
        fun create(
            station: StationDto,
            source: PlaybackSource,
            baseUrl: String,
        ): MediaItem {
            val metadata =
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist("Tolocha Radio")
                    .setArtworkUri(station.favicon?.let { Uri.parse(it) })
                    .setAlbumTitle("Tolocha Radio")
                    .build()
            val builder =
                MediaItem.Builder()
                    .setUri(uriFor(source, baseUrl))
                    .setMediaMetadata(metadata)
            mimeTypeFor(source)?.let(builder::setMimeType)
            return builder.build()
        }

        /** Crea la `MediaSource` adecuada al tipo de fuente. */
        fun createMediaSource(
            item: MediaItem,
            source: PlaybackSource,
        ): MediaSource =
            when (source) {
                is PlaybackSource.DirectHls ->
                    HlsMediaSource.Factory(directDataSource).createMediaSource(item)
                is PlaybackSource.Proxied ->
                    ProgressiveMediaSource.Factory(authDataSource).createMediaSource(item)
                is PlaybackSource.DirectProgressive ->
                    ProgressiveMediaSource.Factory(directDataSource).createMediaSource(item)
            }
    }
