package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode

/**
 * Insignia de episodios pendientes de la cola (spec 0047, US5).
 * No cuenta el episodio en curso; con más de 99 se compacta.
 */
data class QueueBadge(val pendingCount: Int) {
    /** Texto de la insignia; `null` si no hay pendientes. */
    val displayText: String?
        get() =
            when {
                pendingCount <= 0 -> null
                pendingCount > MAX_DISPLAY -> OVERFLOW_TEXT
                else -> pendingCount.toString()
            }

    /** Descripción accesible, en singular o plural. */
    val contentDescription: String
        get() =
            if (pendingCount == 1) {
                "1 episodio en cola"
            } else {
                "$pendingCount episodios en cola"
            }

    companion object {
        const val MAX_DISPLAY = 99
        const val OVERFLOW_TEXT = "99+"
    }
}

/** Cuenta los pendientes excluyendo el episodio que está sonando. */
fun queueBadgeFor(
    items: List<PodcastEpisode>,
    playingId: Long?,
): QueueBadge = QueueBadge(items.count { it.id != playingId })
