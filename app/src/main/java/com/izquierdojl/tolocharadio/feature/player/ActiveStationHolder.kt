package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holder singleton para preservar la emisora activa entre recreaciones
 * de Activity/ViewModel (spec 010, ActiveStationHolder).
 *
 * Lifecycle: se escribe en [PlayerViewModel.play] y se limpia en
 * [PlayerViewModel.stop] / [PlayerViewModel.cancelLoad]. Se lee en
 * el init del ViewModel para sincronizar el estado del ExoPlayer
 * compartido al recrearse la Activity.
 */
@Singleton
class ActiveStationHolder
    @Inject
    constructor() {
        var station: StationDto? = null
            private set

        var playerState: PlayerStateType = PlayerStateType.IDLE
            private set

        fun update(station: StationDto?, state: PlayerStateType) {
            this.station = station
            this.playerState = state
        }

        fun clear() {
            station = null
            playerState = PlayerStateType.IDLE
        }
    }

/** Estado textual del player para persistencia entre recreaciones. */
enum class PlayerStateType {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR,
}
