package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.izquierdojl.tolocharadio.cast.CastPlayerState
import com.izquierdojl.tolocharadio.core.ui.playback.NowPlayingUi
import com.izquierdojl.tolocharadio.core.ui.playback.StationPlaybackUiState

/** Estado efectivo: Cast manda cuando hay sesión remota. */
fun effectivePlayerState(
    local: PlayerState,
    cast: CastPlayerState,
): PlayerState = (cast as? CastPlayerState.Cast)?.playerState ?: local

/** Traduce el estado sellado del player al modelo pequeño de tarjetas. */
fun nowPlayingUi(state: PlayerState): NowPlayingUi =
    when (state) {
        PlayerState.Idle -> NowPlayingUi()
        is PlayerState.Playing ->
            NowPlayingUi(state.station.id, StationPlaybackUiState.Playing)
        is PlayerState.Paused ->
            NowPlayingUi(state.station.id, StationPlaybackUiState.Paused)
        is PlayerState.Buffering ->
            NowPlayingUi(state.station.id, StationPlaybackUiState.Loading)
        is PlayerState.Reconnecting ->
            NowPlayingUi(state.station.id, StationPlaybackUiState.Loading)
        is PlayerState.Error ->
            NowPlayingUi(state.station?.id, StationPlaybackUiState.Error)
    }

/** Variante que resuelve antes el estado efectivo de Cast. */
fun nowPlayingUi(
    local: PlayerState,
    cast: CastPlayerState,
): NowPlayingUi = nowPlayingUi(effectivePlayerState(local, cast))

/** Colecciona estado local + Cast y lo reduce a [NowPlayingUi]. */
@Composable
fun rememberNowPlayingUi(player: PlayerViewModel): NowPlayingUi {
    val local by player.state.collectAsState()
    val cast by player.castState.collectAsState()
    return remember(local, cast) { nowPlayingUi(local, cast) }
}
