package com.izquierdojl.tolocharadio.feature.favorites

/** Milisegundos por unidad para la fecha relativa (FR-001). */
private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val DAYS_PER_WEEK = 7L
private const val DAYS_PER_MONTH = 30L
private const val DAYS_PER_YEAR = 365L

/** Fecha relativa en español para `addedAt` (FR-001). Nulo = ocultar. */
fun relativeTime(
    addedAtMs: Long,
    nowMs: Long = System.currentTimeMillis(),
): String? {
    if (addedAtMs <= 0 || addedAtMs > nowMs) return null
    val diff = nowMs - addedAtMs
    val minute = MILLIS_PER_MINUTE
    val hour = MINUTES_PER_HOUR * minute
    val day = HOURS_PER_DAY * hour
    val week = DAYS_PER_WEEK * day
    val month = DAYS_PER_MONTH * day
    val year = DAYS_PER_YEAR * day
    return when {
        diff < minute -> "ahora mismo"
        diff < hour -> "hace ${diff / minute} min"
        diff < day -> "hace ${diff / hour} h"
        diff < week -> plural(diff / day, "día", "días")
        diff < month -> plural(diff / week, "semana", "semanas")
        diff < year -> plural(diff / month, "mes", "meses")
        else -> plural(diff / year, "año", "años")
    }
}

private fun plural(
    n: Long,
    one: String,
    many: String,
): String = if (n == 1L) "hace 1 $one" else "hace $n $many"

