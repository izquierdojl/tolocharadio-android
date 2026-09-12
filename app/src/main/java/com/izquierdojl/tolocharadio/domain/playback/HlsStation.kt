package com.izquierdojl.tolocharadio.domain.playback

import java.net.URI

/**
 * Detección de emisoras HLS por la extensión de su enlace (spec 0021).
 *
 * Es la única lógica de formato que conserva el cliente: sirve para elegir el
 * motor de reproducción (HLS vs progresivo). La URI de reproducción siempre
 * apunta al proxy, así que no se usa para decidir la ruta.
 */
object HlsStation {
    /**
     * `true` si el path de [url] termina en `.m3u8` (ignorando query y
     * fragment, sin distinguir mayúsculas). URL en blanco o inválida → `false`.
     */
    fun isHls(url: String): Boolean {
        if (url.isBlank()) return false
        val path = runCatching { URI(url).path }.getOrNull() ?: return false
        val lastSegment = path.substringAfterLast('/')
        if (!lastSegment.contains('.')) return false
        return lastSegment.substringAfterLast('.').equals("m3u8", ignoreCase = true)
    }
}
