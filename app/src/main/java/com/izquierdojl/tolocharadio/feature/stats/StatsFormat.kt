package com.izquierdojl.tolocharadio.feature.stats

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Formato de tiempos y etiquetas de las gráficas en español (R8),
 * espejo de `apps/web/src/lib/stats.ts`. Puro, sin Android, testeable
 * sobre la JVM.
 */
object StatsFormat {
    private val ES: Locale = Locale.forLanguageTag("es")
    private val DAY_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", ES)
    private val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", ES)
    private val FULL_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ES)

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MINUTES_PER_HOUR = 60L

    /**
     * Tiempo legible con redondeo al minuto: `0 min`, `N min`, `H h` o
     * `H h M min` (paridad con `formatDurationMs` de la web).
     */
    fun formatDurationMs(ms: Long): String {
        val totalMin = (ms / MILLIS_PER_MINUTE.toDouble()).roundToLong()
        val hours = totalMin / MINUTES_PER_HOUR
        val minutes = totalMin % MINUTES_PER_HOUR
        return when {
            totalMin < 1L -> "0 min"
            totalMin < MINUTES_PER_HOUR -> "$totalMin min"
            minutes == 0L -> "$hours h"
            else -> "$hours h $minutes min"
        }
    }

    /**
     * Etiqueta del intervalo de la serie temporal según la granularidad:
     * `2 sept` (day), `sem. 7 sept` (week), `sept 2026` (month).
     * Devuelve `bucket` tal cual si no es una fecha parseable.
     */
    fun formatBucketLabel(
        bucket: String,
        granularity: String,
    ): String {
        val date = parseDay(bucket) ?: return bucket
        return when (granularity) {
            "month" -> MONTH_LABEL.format(date)
            "week" -> "sem. " + DAY_LABEL.format(date)
            else -> DAY_LABEL.format(date)
        }
    }

    /** Fecha larga en español (`miércoles, 2 de septiembre`); `bucket` si no es parseable. */
    fun formatFullDay(bucket: String): String {
        val date = parseDay(bucket) ?: return bucket
        return FULL_DAY.format(date)
    }

    private fun parseDay(bucket: String): LocalDate? = runCatching { LocalDate.parse(bucket) }.getOrNull()
}
