package com.izquierdojl.tolocharadio.domain.playback

/**
 * Traduce el motivo (`reason`) que devuelve el servicio en
 * `GET /playback/:id/status` a un mensaje accionable en español
 * (spec 0021, FR-006). Nunca expone el código técnico crudo ni el `message`
 * del servidor (constitución IV).
 */
object PlaybackStatusReason {
    /** La lista no contiene ninguna entrada reproducible. */
    const val PLAYLIST_EMPTY = "PLAYLIST_EMPTY"

    /** La lista solo ofrece entradas no cifradas (HTTP). */
    const val PLAYLIST_INSECURE_ONLY = "PLAYLIST_INSECURE_ONLY"

    /** El cuerpo no tiene una gramática de lista reconocible. */
    const val PLAYLIST_MALFORMED = "PLAYLIST_MALFORMED"

    /** No se pudo descargar la lista o el manifiesto. */
    const val PLAYLIST_UNREACHABLE = "PLAYLIST_UNREACHABLE"

    /** El stream no está disponible. */
    const val STREAM_UNAVAILABLE = "STREAM_UNAVAILABLE"

    /**
     * Mensaje accionable para [reason]; `null` o desconocido → mensaje
     * genérico. Todos conservan la opción de reintento en la UI.
     */
    fun reasonToMessage(reason: String?): String =
        when (reason) {
            PLAYLIST_EMPTY -> "La lista de la emisora no contiene ninguna emisión reproducible."
            PLAYLIST_INSECURE_ONLY ->
                "La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app."
            PLAYLIST_MALFORMED -> "La lista de la emisora no tiene un formato válido."
            PLAYLIST_UNREACHABLE -> "No se pudo descargar la lista de la emisora. Comprueba tu conexión."
            STREAM_UNAVAILABLE -> "La emisora no está disponible en este momento."
            else -> "Emisora no disponible."
        }
}
