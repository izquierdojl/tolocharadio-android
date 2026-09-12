package com.izquierdojl.tolocharadio.domain.playback

/**
 * Motivo tipado por el que una emisora de lista no se puede reproducir
 * (spec 0019, FR-009 y FR-013). Permite distinguir un fallo de resolución
 * de uno de red sin exponer detalles técnicos (constitución IV).
 */
enum class PlaybackError {
    /** No se pudo descargar la lista (red). */
    NETWORK,

    /** La lista no contiene entradas reproducibles. */
    NO_ENTRIES,

    /** La lista solo ofrece entradas no cifradas (HTTP), bloqueadas por seguridad. */
    INSECURE_ONLY,

    /** El texto no tiene un formato de lista reconocible. */
    MALFORMED,
    ;

    /** Mensaje accionable en español para la UI. */
    fun userMessage(): String =
        when (this) {
            NETWORK -> "No se pudo descargar la lista. Comprueba tu conexión."
            NO_ENTRIES -> "La lista de la emisora no contiene ninguna emisión reproducible."
            INSECURE_ONLY -> "La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app."
            MALFORMED -> "La lista de la emisora no tiene un formato válido."
        }
}
