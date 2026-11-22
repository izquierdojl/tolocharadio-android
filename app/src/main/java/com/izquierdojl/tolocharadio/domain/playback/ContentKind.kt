package com.izquierdojl.tolocharadio.domain.playback

/**
 * Tipo de contenido que posee el reproductor local (spec 0058, FR-001).
 *
 * Media3 fija el `LoadControl` al construir el `ExoPlayer`, así que la app
 * mantiene un player por tipo: [LIVE] con el perfil de radio del usuario y
 * [VOD] con el buffer profundo de episodios.
 */
enum class ContentKind {
    LIVE,
    VOD,
}
