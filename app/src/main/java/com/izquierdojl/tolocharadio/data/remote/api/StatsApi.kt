package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.StatsCountryListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsGenreListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsHabitListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsRecentListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Estadísticas de escucha del usuario autenticado (Bearer), paridad con
 * la página de gráficas de la web. `from`/`to` en `YYYY-MM-DD` con
 * `from <= to`; `limit` 1..50 (defecto 10) en top/genres y 1..200
 * (defecto 50) en recent; `granularity` day|week|month (defecto day).
 * Contrato completo en `contracts/stats-api.md`.
 */
interface StatsApi {
    @GET("stats/me/top")
    suspend fun top(
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("limit") limit: Int?,
    ): Response<StatsTopListDto>

    @GET("stats/me/timeline")
    suspend fun timeline(
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("granularity") granularity: String?,
    ): Response<StatsTimelineDto>

    @GET("stats/me/habits")
    suspend fun habits(
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<StatsHabitListDto>

    @GET("stats/me/genres")
    suspend fun genres(
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("limit") limit: Int?,
    ): Response<StatsGenreListDto>

    @GET("stats/me/countries")
    suspend fun countries(
        @Query("from") from: String?,
        @Query("to") to: String?,
    ): Response<StatsCountryListDto>

    @GET("stats/me/recent")
    suspend fun recent(
        @Query("limit") limit: Int?,
    ): Response<StatsRecentListDto>
}
