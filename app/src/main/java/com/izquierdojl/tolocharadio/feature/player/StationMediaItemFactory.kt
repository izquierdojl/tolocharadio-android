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
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Único punto de construcción de `MediaItem`/`MediaSource` para reproducción
 * local, Cast y reanudación post-Cast (spec 0019, research R7).
 *
 * La reproducción local usa **siempre** el proxy del servidor (spec 0021/0022):
 * HLS con `HlsMediaSource` sobre el manifiesto reescrito y el resto con
 * `ProgressiveMediaSource`. Para **Cast** se usa la URL pública de la emisora
 * ([createForCast]), porque el receptor no puede autenticarse contra el proxy.
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

        /**
         * URI para Cast: la **URL pública** de la emisora. El receptor descarga
         * el stream por su cuenta y no puede enviar `Authorization`, así que el
         * proxy autenticado no le sirve. Si la emisora no tuviera URL, cae al
         * proxy (comportamiento anterior).
         */
        fun castUriFor(
            station: StationDto,
            fallbackBaseUrl: String,
        ): String = station.url.ifBlank { streamUrl(fallbackBaseUrl, station.id) }

        /**
         * `mimeType` del `MediaItem`. **Nunca `null`**: `CastPlayer` exige que el
         * item lo especifique (`DefaultMediaItemConverter`); sin él la app se
         * cierra al enviar a Cast. HLS → `application/x-mpegURL`; progresivo →
         * según la extensión de la emisora, con `audio/mpeg` por defecto.
         */
        fun mimeTypeFor(
            station: StationDto,
            source: PlaybackSource,
        ): String = if (source.hls) MimeTypes.APPLICATION_M3U8 else audioMimeTypeFor(station.url)

        private fun audioMimeTypeFor(url: String): String {
            val extension =
                runCatching { URI(url).path }
                    .getOrNull()
                    ?.substringAfterLast('/')
                    ?.substringAfterLast('.', "")
                    ?.lowercase()
                    .orEmpty()
            return when (extension) {
                "aac" -> MimeTypes.AUDIO_AAC
                "m4a", "mp4" -> MimeTypes.AUDIO_MP4
                "ogg", "oga" -> MimeTypes.AUDIO_OGG
                "opus" -> MimeTypes.AUDIO_OPUS
                "flac" -> MimeTypes.AUDIO_FLAC
                else -> MimeTypes.AUDIO_MPEG
            }
        }

        /** Construye el `MediaItem` con metadata y `mimeType` (siempre presente). */
        fun create(
            station: StationDto,
            source: PlaybackSource,
            baseUrl: String,
        ): MediaItem =
            MediaItem.Builder()
                .setUri(uriFor(source, baseUrl))
                .setMediaMetadata(metadataFor(station))
                .setMimeType(mimeTypeFor(station, source))
                .build()

        /**
         * `MediaItem` para Cast: usa la **URL pública** de la emisora porque el
         * receptor descarga el stream por su cuenta y no puede autenticarse
         * contra el proxy (`Authorization: Bearer`). Conserva el `mimeType`.
         */
        fun createForCast(
            station: StationDto,
            source: PlaybackSource,
            fallbackBaseUrl: String,
        ): MediaItem =
            MediaItem.Builder()
                .setUri(castUriFor(station, fallbackBaseUrl))
                .setMediaMetadata(metadataFor(station))
                .setMimeType(mimeTypeFor(station, source))
                .build()

        private fun metadataFor(station: StationDto): MediaMetadata =
            MediaMetadata.Builder()
                .setTitle(station.name)
                .setArtist("Tolocha Radio")
                .setArtworkUri(station.favicon?.let { Uri.parse(it) })
                .setAlbumTitle("Tolocha Radio")
                .build()

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
