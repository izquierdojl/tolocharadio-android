package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource

/**
 * Cola de candidatos de una emisora de lista con reintento acotado
 * (spec 0019, FR-007 y caso borde de fallback). Pura y testeable en JVM;
 * el tope de saltos lo aplica el llamante.
 */
class PlaylistPlaybackQueue(
    private val sources: List<PlaybackSource>,
) {
    private var index = 0

    init {
        require(sources.isNotEmpty()) { "PlaylistPlaybackQueue requiere al menos un candidato" }
    }

    /** Candidato actualmente en reproducción. */
    val current: PlaybackSource
        get() = sources[index]

    /** `true` si ya se está en el último candidato. */
    val exhausted: Boolean
        get() = index >= sources.lastIndex

    /** Avanza al siguiente candidato; `false` si ya estaba en el último. */
    fun advance(): Boolean {
        if (exhausted) return false
        index++
        return true
    }
}
