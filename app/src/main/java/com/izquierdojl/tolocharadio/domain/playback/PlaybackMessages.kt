package com.izquierdojl.tolocharadio.domain.playback

/** Textos de error compartidos por el reproductor y la ficha (spec 0044). */
object PlaybackMessages {
    const val UNSUPPORTED_FORMAT = "Esta emisora usa un formato que este reproductor no admite."
    const val STATION_UNAVAILABLE = "La emisora no está disponible en este momento."
    const val RECONNECTION_FAILED =
        "No se pudo recuperar la conexión. Comprueba tu red e inténtalo de nuevo."
    const val AUTH_EXPIRED = "La sesión del servidor ha caducado. Edita el servidor para continuar."
    const val PLAYBACK_INTERRUPTED = "Se ha interrumpido la reproducción."
    const val OFFLINE = "Sin conexión. Se reanudará al volver la red."
}
