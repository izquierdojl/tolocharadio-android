package com.izquierdojl.tolocharadio.domain.playback

/**
 * Configuración de activación de la reproducción continua (spec 0044, FR-021).
 *
 * Es una bandera de compilación, no una preferencia de usuario y no se persiste.
 */
data class PlaybackFeatureFlags(
    val livePlaybackEnabled: Boolean,
) {
    /** Ruta inicial por defecto según la bandera. */
    val defaultRoute: PlaybackRoute
        get() = if (livePlaybackEnabled) PlaybackRoute.CONTINUOUS else PlaybackRoute.LEGACY
}
