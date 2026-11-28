package com.izquierdojl.tolocharadio.core.ui.playback

import androidx.compose.runtime.Immutable

/** Estado de presentación de una emisora dentro de tarjetas y filas. */
enum class StationPlaybackUiState {
    Inactive,
    Playing,
    Paused,
    Loading,
    Error,
}

/**
 * Estado "ahora suena" listo para la UI. [stationId] es nulo cuando no hay
 * reproducción o el error no conserva la emisora.
 */
@Immutable
data class NowPlayingUi(
    val stationId: String? = null,
    val state: StationPlaybackUiState = StationPlaybackUiState.Inactive,
) {
    /** Devuelve [Inactive] si la emisora no es la activa. */
    fun stateFor(stationId: String): StationPlaybackUiState {
        return if (this.stationId == stationId) state else StationPlaybackUiState.Inactive
    }
}
