package com.izquierdojl.tolocharadio.feature.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.izquierdojl.tolocharadio.data.remote.api.liveStreamUrl
import com.izquierdojl.tolocharadio.data.remote.api.streamUrl
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Único punto de construcción de `MediaItem`/`MediaSource` para reproducción
 * local, Cast y reanudación post-Cast (spec 0019/0044).
 *
 * La ruta `CONTINUOUS` usa `/playback/{id}/live` y una `ProgressiveMediaSource`
 * sin MIME local. La ruta `LEGACY` conserva el manifiesto HLS o el progresivo
 * anterior. Para Cast se usa la URL pública de la emisora.
 */
@OptIn(UnstableApi::class)
@Singleton
@Suppress("TooManyFunctions")
class StationMediaItemFactory
    @Inject
    constructor(
        private val playerDataSource: PlayerDataSourceFactory,
        private val plainDataSource: DataSource.Factory,
    ) {
        /** URL efectiva de la ruta legacy: proxy del servidor. */
        fun uriFor(
            source: PlaybackSource,
            baseUrl: String,
        ): String = streamUrl(baseUrl, source.stationId)

        /** URL de la ruta continua, con marcador anticaché opcional. */
        fun liveUriFor(
            stationId: String,
            baseUrl: String,
            sequence: Long? = null,
        ): String = liveStreamUrl(baseUrl, stationId, sequence?.toInt())

        /** URL de la ruta legacy, con marcador anticaché opcional. */
        fun legacyUriFor(
            source: PlaybackSource,
            baseUrl: String,
            sequence: Long? = null,
        ): String = streamUrl(baseUrl, source.stationId, sequence?.toInt())

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
         * `mimeType` del `MediaItem` legacy. **Nunca `null`** para Cast:
         * `CastPlayer` exige que el item lo especifique. HLS → `application/x-mpegURL`;
         * progresivo → según la extensión de la emisora, `audio/mpeg` por defecto.
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

        /** `MediaItem` de la ruta legacy (compatibilidad/Cast). */
        fun create(
            station: StationDto,
            source: PlaybackSource,
            baseUrl: String,
        ): MediaItem = createLegacy(station, source, baseUrl)

        /** `MediaItem` de la ruta continua: sin MIME local, extractor por Content-Type. */
        fun createLive(
            station: StationDto,
            baseUrl: String,
            sequence: Long? = null,
        ): MediaItem =
            MediaItem.Builder()
                .setUri(liveUriFor(station.id, baseUrl, sequence))
                .setMediaMetadata(metadataFor(station))
                .build()

        /** `MediaItem` de la ruta legacy, con MIME explícito. */
        fun createLegacy(
            station: StationDto,
            source: PlaybackSource,
            baseUrl: String,
            sequence: Long? = null,
        ): MediaItem =
            MediaItem.Builder()
                .setUri(legacyUriFor(source, baseUrl, sequence))
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

        /** `MediaSource` VOD de episodio (URL firmada o archivo local, research D3). */
        fun createEpisodeSource(item: MediaItem): MediaSource {
            val factory = ProgressiveMediaSource.Factory(plainDataSource)
            return factory.createMediaSource(item)
        }

        /** `mimeType` para un audio sin extensión conocida (URLs firmadas). */
        fun audioMimeTypeForUrl(url: String): String = audioMimeTypeFor(url)

        /**
         * `MediaItem` de episodio para Cast (spec 0046, T042): la URL firmada
         * viaja en el propio item y el receptor no necesita autenticarse.
         */
        fun createEpisodeCastItem(
            uri: String,
            title: String,
            artist: String?,
        ): MediaItem =
            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist ?: "Tolocha Podcasts")
                        .build(),
                )
                .setMimeType(audioMimeTypeFor(uri))
                .build()

        /** Crea la `MediaSource` según la ruta: progresiva continua, legacy o episodio VOD. */
        fun createMediaSource(
            item: MediaItem,
            source: PlaybackSource,
            route: PlaybackRoute,
        ): MediaSource =
            when (route) {
                PlaybackRoute.CONTINUOUS ->
                    ProgressiveMediaSource.Factory(playerDataSource).createMediaSource(item)
                PlaybackRoute.LEGACY ->
                    if (source.hls) {
                        HlsMediaSource.Factory(playerDataSource).createMediaSource(item)
                    } else {
                        ProgressiveMediaSource.Factory(playerDataSource).createMediaSource(item)
                    }
                // Episodio: fichero finito vía URL firmada, sin Bearer (research D3).
                PlaybackRoute.EPISODE ->
                    ProgressiveMediaSource.Factory(plainDataSource).createMediaSource(item)
            }

        /** Compatibilidad con llamadas legacy: HLS o progresivo. */
        fun createMediaSource(
            item: MediaItem,
            source: PlaybackSource,
        ): MediaSource = createMediaSource(item, source, PlaybackRoute.LEGACY)
    }
