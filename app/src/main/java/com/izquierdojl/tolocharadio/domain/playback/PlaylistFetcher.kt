package com.izquierdojl.tolocharadio.domain.playback

/** Contenido descargado de una lista de reproducción de texto. */
data class PlaylistContent(
    /** Cuerpo de la lista. */
    val text: String,
    /** URL final tras seguir redirecciones; base para resolver relativas. */
    val finalUrl: String,
)

/**
 * Descarga el texto de una lista **sin credenciales**: el token Bearer nunca
 * viaja a hosts de terceros (FR-010, constitución II).
 */
fun interface PlaylistFetcher {
    /**
     * Descarga [url] y devuelve su contenido.
     *
     * @throws PlaylistFetchException si la descarga falla.
     */
    suspend fun fetch(url: String): PlaylistContent
}

/** Fallo controlado al descargar una lista; se mapea a `PlaybackError.NETWORK`. */
class PlaylistFetchException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
