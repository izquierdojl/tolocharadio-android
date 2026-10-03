package com.izquierdojl.tolocharadio.domain.stats

import com.izquierdojl.tolocharadio.data.remote.dto.StatsHabitEntryDto

private const val DAYS_PER_WEEK = 7
private const val HOURS_PER_DAY = 24

/**
 * Matriz 7×24 (día 0..6 = lunes..domingo × hora 0..23) con el tiempo
 * escuchado por celda (paridad con `buildHabitsMatrix` de la web).
 * Las entradas fuera de rango se ignoran.
 */
fun buildHabitsMatrix(items: List<StatsHabitEntryDto>): List<List<Long>> {
    val matrix = List(DAYS_PER_WEEK) { LongArray(HOURS_PER_DAY) }
    for (item in items) {
        if (item.weekday !in 0 until DAYS_PER_WEEK || item.hour !in 0 until HOURS_PER_DAY) continue
        matrix[item.weekday][item.hour] += item.totalMs
    }
    return matrix.map { row -> row.toList() }
}
