package com.izquierdojl.tolocharadio.domain.stats

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Rango de fechas `YYYY-MM-DD` (anulables) para las consultas de estadísticas. */
data class StatsRange(val from: String?, val to: String?)

/** Filtro de periodo de la vista (FR-003): 7/30/90 días o todo el histórico; defecto 30. */
enum class StatsPeriod(val days: Int?) {
    SEVEN(7),
    THIRTY(30),
    NINETY(90),
    ALL(null),
    ;

    /**
     * Rango inclusivo del periodo para la fecha local [today]
     * (paridad con `presetRange` de la web); sin fechas en [ALL].
     */
    fun range(today: LocalDate = LocalDate.now()): StatsRange {
        val to = DAY_FORMAT.format(today)
        val days = days ?: return StatsRange(null, null)
        val from = DAY_FORMAT.format(today.minusDays((days - 1).toLong()))
        return StatsRange(from, to)
    }

    private companion object {
        val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

/** Granularidad automática de la serie temporal (FR-004, R5). */
enum class TimelineGranularity(val wire: String) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
}

/** Días del rango según `daysBetween` de la web; sin fechas completas → 30. */
fun StatsRange.days(): Int {
    val fromDate = from ?: return DEFAULT_DAYS
    val toDate = to ?: return DEFAULT_DAYS
    val start = runCatching { LocalDate.parse(fromDate) }.getOrNull() ?: return DEFAULT_DAYS
    val end = runCatching { LocalDate.parse(toDate) }.getOrNull() ?: return DEFAULT_DAYS
    return (ChronoUnit.DAYS.between(start, end) + 1L).toInt().coerceAtLeast(1)
}

/**
 * Granularidad del rango (R5, paridad con `autoGranularity` de la web):
 * hasta 62 días → día, hasta 370 → semana, si no → mes.
 */
fun StatsRange.autoGranularity(): TimelineGranularity {
    val dayCount = days()
    return when {
        dayCount <= MAX_DAYS_PER_DAY_GRANULARITY -> TimelineGranularity.DAY
        dayCount <= MAX_DAYS_PER_WEEK_GRANULARITY -> TimelineGranularity.WEEK
        else -> TimelineGranularity.MONTH
    }
}

private const val DEFAULT_DAYS = 30
private const val MAX_DAYS_PER_DAY_GRANULARITY = 62
private const val MAX_DAYS_PER_WEEK_GRANULARITY = 370
