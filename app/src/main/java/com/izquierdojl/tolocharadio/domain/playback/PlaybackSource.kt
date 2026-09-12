package com.izquierdojl.tolocharadio.domain.playback

/**
 * Fuente de reproducción decidida para una emisora (spec 0019).
 *
 * Invariante: [DirectProgressive] y [DirectHls] solo contienen URLs `https`
 * (FR-010). [Proxied] se resuelve con `streamUrl(baseUrl, stationId)` en la
 * capa de presentación, que es quien conoce la `baseUrl`.
 */
sealed interface PlaybackSource {
    /** Emisora normal: proxy autenticado `GET /playback/:id` con Bearer. */
    data class Proxied(val stationId: String) : PlaybackSource

    /** Entrada directa no HLS (audio progresivo). */
    data class DirectProgressive(val url: String) : PlaybackSource

    /** Recurso HLS (`.m3u8`) gestionado por Media3. */
    data class DirectHls(val url: String) : PlaybackSource
}
