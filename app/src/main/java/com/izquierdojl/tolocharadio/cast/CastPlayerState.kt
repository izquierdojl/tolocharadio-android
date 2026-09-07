package com.izquierdojl.tolocharadio.cast

import com.izquierdojl.tolocharadio.feature.player.PlayerState

sealed interface CastPlayerState {
    data class Local(val playerState: PlayerState) : CastPlayerState

    data class Cast(
        val playerState: PlayerState,
        val deviceName: String,
        val connectionState: CastConnectionState,
    ) : CastPlayerState
}
