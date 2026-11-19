package com.izquierdojl.tolocharadio.feature.player

/** Conjunto de botones de transporte que debe publicar la sesión. */
enum class MediaButtonLayout {
    /** Radio en directo: play/pausa + stop/mute; sin seek. */
    RADIO,

    /** Radio con fallo recuperable: añade reintentar. */
    RADIO_RETRY,

    /** Episodio local o en Cast: retroceder/avanzar + stop/mute. */
    EPISODE,
}

/**
 * Política pura de botones de la notificación (FR-007–FR-013).
 * El contenido de episodio siempre tiene prioridad; la radio nunca
 * publica botones de seek.
 */
object MediaButtonPolicy {
    fun layout(
        episodeActive: Boolean,
        radioRetry: Boolean,
    ): MediaButtonLayout =
        when {
            episodeActive -> MediaButtonLayout.EPISODE
            radioRetry -> MediaButtonLayout.RADIO_RETRY
            else -> MediaButtonLayout.RADIO
        }
}
