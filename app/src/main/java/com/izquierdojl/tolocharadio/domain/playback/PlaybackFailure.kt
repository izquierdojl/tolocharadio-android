package com.izquierdojl.tolocharadio.domain.playback

/** Error normalizado de reproducción (spec 0044). */
data class PlaybackFailure(
    val cause: InterruptionCause,
    val httpStatus: Int? = null,
    val playerErrorCode: Int? = null,
)
