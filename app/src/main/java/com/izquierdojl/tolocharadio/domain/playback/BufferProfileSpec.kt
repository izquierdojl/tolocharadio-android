package com.izquierdojl.tolocharadio.domain.playback

/**
 * Parámetros de `DefaultLoadControl` para un [BufferProfile] (spec 0042, R1).
 *
 * Invariantes: todos los valores son positivos, `minBufferMs` cubre los umbrales
 * de inicio/reanudación, `maxBufferMs >= minBufferMs` y el rebuffer no supera
 * 7 s para cumplir la reanudación en menos de 10 s.
 */
data class BufferProfileSpec(
    val minBufferMs: Long,
    val maxBufferMs: Long,
    val bufferForPlaybackMs: Long,
    val bufferForPlaybackAfterRebufferMs: Long,
) {
    init {
        require(minBufferMs > 0) { "minBufferMs debe ser > 0" }
        require(maxBufferMs >= minBufferMs) { "maxBufferMs debe ser >= minBufferMs" }
        require(bufferForPlaybackMs > 0) { "bufferForPlaybackMs debe ser > 0" }
        require(bufferForPlaybackAfterRebufferMs > 0) { "bufferForPlaybackAfterRebufferMs debe ser > 0" }
        require(minBufferMs >= bufferForPlaybackMs) { "minBufferMs debe cubrir bufferForPlaybackMs" }
        require(minBufferMs >= bufferForPlaybackAfterRebufferMs) {
            "minBufferMs debe cubrir bufferForPlaybackAfterRebufferMs"
        }
        require(bufferForPlaybackAfterRebufferMs <= MAX_REBUFFER_MS) {
            "bufferForPlaybackAfterRebufferMs no debe superar $MAX_REBUFFER_MS ms"
        }
    }

    companion object {
        const val MAX_REBUFFER_MS: Long = 7_000

        /** Valores de producto acordados en research R1. */
        @Suppress("MagicNumber")
        fun forProfile(profile: BufferProfile): BufferProfileSpec =
            when (profile) {
                BufferProfile.DATA_SAVER ->
                    BufferProfileSpec(
                        minBufferMs = 10_000,
                        maxBufferMs = 30_000,
                        bufferForPlaybackMs = 1_000,
                        bufferForPlaybackAfterRebufferMs = 2_000,
                    )
                BufferProfile.BALANCED ->
                    BufferProfileSpec(
                        minBufferMs = 20_000,
                        maxBufferMs = 50_000,
                        bufferForPlaybackMs = 2_000,
                        bufferForPlaybackAfterRebufferMs = 4_000,
                    )
                BufferProfile.MAX_STABILITY ->
                    BufferProfileSpec(
                        minBufferMs = 40_000,
                        maxBufferMs = 60_000,
                        bufferForPlaybackMs = 6_000,
                        bufferForPlaybackAfterRebufferMs = 7_000,
                    )
            }
    }
}
