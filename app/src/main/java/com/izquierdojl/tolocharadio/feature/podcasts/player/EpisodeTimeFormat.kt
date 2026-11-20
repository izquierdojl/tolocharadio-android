package com.izquierdojl.tolocharadio.feature.podcasts.player

/**
 * Formato compacto de tiempos del reproductor: `m:ss` por debajo de
 * una hora y `h:mm:ss` a partir de una hora.
 */
fun formatPlaybackTime(positionMs: Long): String {
    val totalSec = (positionMs / MS_PER_SEC).coerceAtLeast(0L)
    val hours = totalSec / SECONDS_PER_HOUR
    val minutes = (totalSec % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSec % SECONDS_PER_MINUTE
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** Tiempo restante con signo menos y el mismo formato compacto. */
fun formatRemainingTime(remainingMs: Long): String = "-" + formatPlaybackTime(remainingMs.coerceAtLeast(0L))

private const val MS_PER_SEC = 1_000L

private const val SECONDS_PER_MINUTE = 60L

private const val SECONDS_PER_HOUR = 3_600L
