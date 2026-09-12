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
 * local, Cast y reanudación post-Cast (spec 0019, research R7).
 *
 * Desde la spec 0022 la URI es **siempre** la del proxy del servidor, sin
 * credenciales: HLS se reproduce con `HlsMediaSource` sobre el manifiesto
 * reescrito y el resto con `ProgressiveMediaSource`.
 */
@OptIn(UnstableApi::class)
@Singleton
class StationMediaItemFactory
    @Inject
    constructor(
        private val playerDataSource: PlayerDataSourceFactory,
    ) {
        /** URL efectiva de la fuente: siempre el proxy del servidor. */
        fun uriFor(
            source: PlaybackSource,
            baseUrl: String,
        ): String = streamUrl(baseUrl, source.stationId)

        /** `mimeType` del `MediaItem`; HLS para emisoras `.m3u8`, ninguno en el resto. */
        fun mimeTypeFor(source: PlaybackSource): String? = if (source.hls) MimeTypes.APPLICATION_M3U8 else null

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

        /** Crea la `MediaSource` adecuada: HLS o progresiva, siempre por proxy. */
        fun createMediaSource(
            item: MediaItem,
            source: PlaybackSource,
        ): MediaSource =
            if (source.hls) {
                HlsMediaSource.Factory(playerDataSource).createMediaSource(item)
            } else {
                ProgressiveMediaSource.Factory(playerDataSource).createMediaSource(item)
            }
    }
