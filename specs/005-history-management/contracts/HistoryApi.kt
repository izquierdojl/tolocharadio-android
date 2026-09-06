package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.HistoryListDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

/** Historial de reproducción (Bearer). */
interface HistoryApi {
    /** Lista el historial del usuario, ordenado por más reciente. */
    @GET("history")
    suspend fun list(): Response<HistoryListDto>

    /** Elimina una emisora específica del historial. */
    @DELETE("history/{stationId}")
    suspend fun remove(
        @Path("stationId") stationId: String,
    ): Response<OkResult>

    /** Limpia todo el historial del usuario. */
    @DELETE("history")
    suspend fun clear(): Response<OkResult>
}

