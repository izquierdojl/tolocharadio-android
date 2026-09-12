package com.izquierdojl.tolocharadio.domain.playback

import java.net.URI

/** Resultado del parseo de una lista de texto (spec 0019). */
sealed interface PlaylistParseResult {
    /** Entradas HTTPS reproducibles, ordenadas y sin duplicados. */
    data class Candidates(val urls: List<String>) : PlaylistParseResult

    /** La lista está vacía o solo tiene comentarios. */
    data object NoEntries : PlaylistParseResult

    /** Había entradas, pero ninguna es HTTPS (FR-010). */
    data object InsecureOnly : PlaylistParseResult

    /** El texto no tiene un formato de lista reconocible. */
    data object Malformed : PlaylistParseResult
}

/**
 * Parser puro de listas M3U/PLS (ver `contracts/playlist-resolution.md` §3-§6).
 *
 * Sin dependencias Android: se testea en JVM. Resuelve URLs relativas contra la
 * URL final de la descarga, descarta esquemas distintos de `https` y deduplica
 * preservando el orden, con un tope de [MAX_CANDIDATES].
 */
object PlaylistParser {
    private const val MAX_CANDIDATES = 5
    private const val HTTPS_PREFIX = "https://"
    private const val FILE_KEY_PREFIX = "File"
    private val EXTENSION_REGEX = Regex("\\.[A-Za-z0-9]{2,5}$")

    /**
     * Parsea [text] asumiendo [format] (nunca [PlaylistFormat.HLS]).
     *
     * @param finalUrl URL efectiva tras redirects, base para resolver relativas.
     */
    fun parse(
        text: String,
        finalUrl: String,
        format: PlaylistFormat,
    ): PlaylistParseResult {
        if (format == PlaylistFormat.HLS) return PlaylistParseResult.NoEntries
        val clean = text.removePrefix("\uFEFF")
        val raw = if (format == PlaylistFormat.M3U) m3uEntries(clean) else plsEntries(clean)
        return when {
            raw.isEmpty() && format == PlaylistFormat.PLS && looksLikePlsWithoutEntries(clean) ->
                PlaylistParseResult.Malformed
            raw.isEmpty() -> PlaylistParseResult.NoEntries
            else -> classify(raw.filter(::looksResolvable).mapNotNull { resolve(it, finalUrl) })
        }
    }

    private fun classify(resolved: List<String>): PlaylistParseResult {
        if (resolved.isEmpty()) return PlaylistParseResult.Malformed
        val https = resolved.filter { it.startsWith(HTTPS_PREFIX, ignoreCase = true) }.distinct()
        return if (https.isEmpty()) {
            PlaylistParseResult.InsecureOnly
        } else {
            PlaylistParseResult.Candidates(https.take(MAX_CANDIDATES))
        }
    }

    private fun m3uEntries(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.substringBefore(' ').trim() }
            .filter { it.isNotEmpty() }
            .toList()

    private fun plsEntries(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith(";") && !it.startsWith("#") }
            .mapNotNull(::plsEntry)
            .sortedBy { it.first }
            .map { it.second }
            .toList()

    private fun plsEntry(line: String): Pair<Int, String>? {
        val separator = line.indexOf('=')
        if (separator <= 0) return null
        val key = line.substring(0, separator).trim()
        val number =
            if (key.startsWith(FILE_KEY_PREFIX, ignoreCase = true)) {
                key.substring(FILE_KEY_PREFIX.length).toIntOrNull()
            } else {
                null
            }
        return number?.let { it to line.substring(separator + 1).trim() }
    }

    private fun looksLikePlsWithoutEntries(clean: String): Boolean {
        val hasContent = clean.isNotBlank()
        val hasMarker = clean.contains("[playlist]", ignoreCase = true)
        return hasContent && !hasMarker
    }

    private fun looksResolvable(value: String): Boolean =
        value.contains("://") ||
            value.startsWith("/") ||
            value.contains('/') ||
            EXTENSION_REGEX.containsMatchIn(value)

    private fun resolve(
        value: String,
        base: String,
    ): String? =
        runCatching {
            val uri = URI(value)
            if (uri.isAbsolute) uri.toString() else URI(base).resolve(uri).toString()
        }.getOrNull()
}
