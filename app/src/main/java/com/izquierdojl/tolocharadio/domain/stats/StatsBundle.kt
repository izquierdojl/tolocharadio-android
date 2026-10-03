package com.izquierdojl.tolocharadio.domain.stats

import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsGenreListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsHabitListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsRecentListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto

/**
 * Conjunto de bloques de gráficas de un periodo (R3): la vista los
 * recarga todos juntos al cambiar de periodo para no conservar datos
 * del periodo anterior (SC-004). Se rellena por historias: US1 `top` y
 * `timeline`, US2 `genres`/`countries`, US3 `habits`/`recent`
 * (bundle completo de R3).
 */
data class StatsBundle(
    val top: StatsTopListDto,
    val timeline: StatsTimelineDto,
    val genres: StatsGenreListDto = StatsGenreListDto(),
    val countries: StatsCountryListDto = StatsCountryListDto(),
    val habits: StatsHabitListDto = StatsHabitListDto(),
    val recent: StatsRecentListDto = StatsRecentListDto(),
)
