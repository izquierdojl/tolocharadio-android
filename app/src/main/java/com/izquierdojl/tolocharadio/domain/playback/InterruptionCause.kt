package com.izquierdojl.tolocharadio.domain.playback

/** Causa de una interrupción de reproducción (spec 0042/0044). */
enum class InterruptionCause {
    NO_CONNECTIVITY,
    STALL,
    NETWORK_ERROR,
    NETWORK_CHANGED,
    STREAM_ENDED,
    AUTH_ERROR,
    STATION_UNAVAILABLE,
    LIVE_ROUTE_UNAVAILABLE,
    UNSUPPORTED_FORMAT,
    DECODER_ERROR,
    UNKNOWN,
}
