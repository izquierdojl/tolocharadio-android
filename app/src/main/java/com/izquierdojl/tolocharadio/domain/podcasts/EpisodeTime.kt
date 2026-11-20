package com.izquierdojl.tolocharadio.domain.podcasts

import kotlin.math.ceil

/** Milisegundos por segundo. */
private const val MS_PER_SEC = 1_000L

/** Segundos por minuto. */
private const val SECONDS_PER_MINUTE = 60L

/**
 * Tiempo restante en milisegundos, acotado a `0..durationMs`
 * (`data-model.md` `EpisodeTimeInfo.remainingMs`). `null` si la
 * duración no se conoce.
 */
fun remainingMs(
    positionMs: Long,
    durationMs: Long,
): Long? {
    if (durationMs <= 0L) return null
    val position = positionMs.coerceIn(0L, durationMs)
    return (durationMs - position).coerceAtLeast(0L)
}

/**
 * Fracción de avance en `0f..1f` (`data-model.md`
 * `EpisodeTimeInfo.progressFraction`). `null` si no hay duración.
 */
fun progressFraction(
    positionMs: Long,
    durationMs: Long,
): Float? {
    if (durationMs <= 0L) return null
    val position = positionMs.coerceIn(0L, durationMs)
    return (position.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

/**
 * Segundos restantes de un episodio empezado y no completado.
 * `null` si no ha empezado, ya terminó o la duración no se conoce.
 */
fun remainingSeconds(
    positionSec: Long,
    durationSec: Long?,
): Long? {
    val duration = durationSec ?: return null
    if (duration <= 0L || positionSec <= 0L || positionSec >= duration) return null
    return duration - positionSec
}

/**
 * Minutos restantes redondeados hacia arriba (`QueueEpisodeMeta.remainingLabel`).
 * `null` si no queda tiempo que mostrar.
 */
fun remainingWholeMinutes(
    positionSec: Long,
    durationSec: Long?,
): Long? {
    val seconds = remainingSeconds(positionSec, durationSec) ?: return null
    return ceil(seconds.toDouble() / SECONDS_PER_MINUTE).toLong().coerceAtLeast(1L)
}

/**
 * Etiqueta breve del restante para una fila de cola:
 * `"Quedan N min"` o `"Queda menos de 1 min"`.
 */
fun remainingLabel(
    positionSec: Long,
    durationSec: Long?,
): String? {
    val seconds = remainingSeconds(positionSec, durationSec) ?: return null
    if (seconds < SECONDS_PER_MINUTE) return "Queda menos de 1 min"
    val minutes = ceil(seconds.toDouble() / SECONDS_PER_MINUTE).toLong()
    return "Quedan $minutes min"
}

/** Convierte segundos a milisegundos para los cálculos de tiempo. */
fun secondsToMillis(seconds: Long): Long = seconds * MS_PER_SEC
