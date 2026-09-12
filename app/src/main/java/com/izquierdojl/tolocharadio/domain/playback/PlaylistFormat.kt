package com.izquierdojl.tolocharadio.domain.playback

import java.net.URI

/**
 * Formato de lista deducido de la URL de una emisora (spec 0019).
 *
 * `HLS` = `.m3u8` (lo gestiona Media3); `M3U`/`PLS` = listas de texto que se
 * resuelven en cliente; `null` = stream directo (pipeline de proxy actual).
 */
enum class PlaylistFormat {
    HLS,
    M3U,
    PLS,
    ;

    companion object {
        /** Devuelve el formato o `null` si es un stream directo o la URL no es válida. */
        fun detect(url: String): PlaylistFormat? {
            if (url.isBlank()) return null
            val path =
                runCatching { URI(url).path }.getOrNull()
                    ?: return null
            if (path.isBlank()) return null
            val lastSegment = path.substringAfterLast('/')
            if (!lastSegment.contains('.')) return null
            return when (lastSegment.substringAfterLast('.').lowercase()) {
                "m3u8" -> HLS
                "m3u" -> M3U
                "pls" -> PLS
                else -> null
            }
        }
    }
}
