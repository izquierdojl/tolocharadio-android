package com.izquierdojl.tolocharadio.domain.playback

/** Causa de una interrupción de reproducción (spec 0042, R4/R6). */
enum class InterruptionCause {
    NO_CONNECTIVITY,
    STALL,
    NETWORK_ERROR,
    AUTH_ERROR,
    STATION_UNAVAILABLE,
    DECODER_ERROR,
    UNKNOWN,
}
