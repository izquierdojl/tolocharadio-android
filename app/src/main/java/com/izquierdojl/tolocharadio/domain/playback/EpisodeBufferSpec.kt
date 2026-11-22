package com.izquierdojl.tolocharadio.domain.playback

/** Fondo mínimo de audio que se intenta mantener en un episodio (2 min). */
private const val EPISODE_MIN_BUFFER_MS = 120_000L

/** Techo de audio por delante en un episodio (4 min). */
private const val EPISODE_MAX_BUFFER_MS = 240_000L

/** Audio necesario para empezar a sonar; no debe retrasar el play. */
private const val EPISODE_BUFFER_FOR_PLAYBACK_MS = 2_000L

/** Audio necesario para reanudar tras un rebuffer. */
private const val EPISODE_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2_500L

/**
 * Parámetros de `DefaultLoadControl` para episodios (spec 0058, FR-002).
 *
 * Invariantes: [minBufferMs] no baja de 2 minutos y cubre los umbrales de
 * arranque y rebuffer, y [maxBufferMs] no es menor que [minBufferMs]. Con
 * 192 kbps, 4 minutos ocupan ~5,6 MB, por debajo del objetivo de audio de
 * Media3 (12,8 MB).
 */
data class EpisodeBufferSpec(
    val minBufferMs: Long = EPISODE_MIN_BUFFER_MS,
    val maxBufferMs: Long = EPISODE_MAX_BUFFER_MS,
    val bufferForPlaybackMs: Long = EPISODE_BUFFER_FOR_PLAYBACK_MS,
    val bufferForPlaybackAfterRebufferMs: Long = EPISODE_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
) {
    init {
        require(minBufferMs >= MIN_MARGIN_MS) { "minBufferMs debe ser >= $MIN_MARGIN_MS" }
        require(maxBufferMs >= minBufferMs) { "maxBufferMs debe ser >= minBufferMs" }
        require(bufferForPlaybackMs > 0) { "bufferForPlaybackMs debe ser > 0" }
        require(bufferForPlaybackAfterRebufferMs > 0) {
            "bufferForPlaybackAfterRebufferMs debe ser > 0"
        }
        require(minBufferMs >= bufferForPlaybackMs) { "minBufferMs debe cubrir bufferForPlaybackMs" }
        require(minBufferMs >= bufferForPlaybackAfterRebufferMs) {
            "minBufferMs debe cubrir bufferForPlaybackAfterRebufferMs"
        }
    }

    companion object {
        /** Margen mínimo de producto: 2 minutos. */
        const val MIN_MARGIN_MS: Long = 120_000

        /** Valores de producto acordados en research R2. */
        val DEFAULT: EpisodeBufferSpec = EpisodeBufferSpec()
    }
}
