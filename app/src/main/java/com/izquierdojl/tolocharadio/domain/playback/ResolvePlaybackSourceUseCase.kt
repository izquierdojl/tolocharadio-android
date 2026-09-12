package com.izquierdojl.tolocharadio.domain.playback

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import javax.inject.Inject

/** Resultado de resolver cómo reproducir una emisora (spec 0019). */
sealed interface ResolutionResult {
    /** Emisora normal: precheck bloqueante + proxy autenticado. */
    data class Proxied(val stationId: String) : ResolutionResult

    /** Un único destino directo (HLS o progresivo). */
    data class Single(val source: PlaybackSource) : ResolutionResult

    /** Varias entradas de lista, en orden (1..5), para fallback acotado. */
    data class Candidates(val sources: List<PlaybackSource>) : ResolutionResult

    /** No reproducible por la vía directa. */
    data class Unavailable(val error: PlaybackError) : ResolutionResult
}

/**
 * Decide la fuente de reproducción de una emisora:
 * - stream directo → [ResolutionResult.Proxied] (pipeline actual),
 * - `.m3u8` → [ResolutionResult.Single] con [PlaybackSource.DirectHls],
 * - `.m3u`/`.pls` → descarga y parseo en cliente ([ResolutionResult.Single] o
 *   [ResolutionResult.Candidates]).
 *
 * Función pura respecto a Android; el acceso a red se delega en [PlaylistFetcher].
 */
class ResolvePlaybackSourceUseCase
    @Inject
    constructor(
        private val fetcher: PlaylistFetcher,
    ) {
        /** Resuelve [station] sin lanzar excepciones de red. */
        suspend operator fun invoke(station: StationDto): ResolutionResult =
            when (val format = PlaylistFormat.detect(station.url)) {
                null -> ResolutionResult.Proxied(station.id)
                PlaylistFormat.HLS -> ResolutionResult.Single(PlaybackSource.DirectHls(station.url))
                PlaylistFormat.M3U, PlaylistFormat.PLS -> resolveList(station.url, format)
            }

        @Suppress("SwallowedException")
        private suspend fun resolveList(
            url: String,
            format: PlaylistFormat,
        ): ResolutionResult {
            val content =
                try {
                    fetcher.fetch(url)
                } catch (e: PlaylistFetchException) {
                    return ResolutionResult.Unavailable(PlaybackError.NETWORK)
                }
            return when (val parsed = PlaylistParser.parse(content.text, content.finalUrl, format)) {
                is PlaylistParseResult.Candidates -> toResult(parsed.urls)
                PlaylistParseResult.NoEntries -> ResolutionResult.Unavailable(PlaybackError.NO_ENTRIES)
                PlaylistParseResult.InsecureOnly -> ResolutionResult.Unavailable(PlaybackError.INSECURE_ONLY)
                PlaylistParseResult.Malformed -> ResolutionResult.Unavailable(PlaybackError.MALFORMED)
            }
        }

        private fun toResult(urls: List<String>): ResolutionResult {
            val sources = urls.map(::toSource)
            return if (sources.size == 1) {
                ResolutionResult.Single(sources.first())
            } else {
                ResolutionResult.Candidates(sources)
            }
        }

        private fun toSource(url: String): PlaybackSource =
            if (PlaylistFormat.detect(url) == PlaylistFormat.HLS) {
                PlaybackSource.DirectHls(url)
            } else {
                PlaybackSource.DirectProgressive(url)
            }
    }
