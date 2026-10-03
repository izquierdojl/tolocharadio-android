package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.safeCall
import com.izquierdojl.tolocharadio.data.remote.api.StatsApi
import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsGenreListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsHabitListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsRecentListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estadísticas de escucha del usuario del servidor activo (Bearer).
 * Sin caché local (R2): la verdad es el servidor y los fallos se
 * reintentan desde la vista. Contrato en `contracts/stats-api.md`.
 */
@Singleton
class StatsRepo
    @Inject
    constructor(
        private val api: StatsApi,
    ) {
        /** Emisoras más escuchadas del periodo (`limit` 1..50, defecto 10). */
        suspend fun top(
            from: String?,
            to: String?,
            limit: Int?,
        ): ApiResult<StatsTopListDto> = safeCall { api.top(from, to, limit) }

        /** Serie temporal del periodo (`granularity` day|week|month). */
        suspend fun timeline(
            from: String?,
            to: String?,
            granularity: String,
        ): ApiResult<StatsTimelineDto> = safeCall { api.timeline(from, to, granularity) }

        /** Matriz de hábitos día×hora (`weekday` 0..6, `hour` 0..23). */
        suspend fun habits(
            from: String?,
            to: String?,
        ): ApiResult<StatsHabitListDto> = safeCall { api.habits(from, to) }

        /** Reparto de escucha por género (`limit` 1..50, defecto 10). */
        suspend fun genres(
            from: String?,
            to: String?,
            limit: Int?,
        ): ApiResult<StatsGenreListDto> = safeCall { api.genres(from, to, limit) }

        /** Reparto de escucha por país. */
        suspend fun countries(
            from: String?,
            to: String?,
        ): ApiResult<StatsCountryListDto> = safeCall { api.countries(from, to) }

        /** Reproducciones recientes (`limit` 1..200, defecto 50). */
        suspend fun recent(limit: Int?): ApiResult<StatsRecentListDto> = safeCall { api.recent(limit) }
    }
