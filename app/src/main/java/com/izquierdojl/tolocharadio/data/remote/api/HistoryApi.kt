package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.HistoryListDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

/** Historial de reproducción, compartido por la instancia. */
interface HistoryApi {
    @GET("history")
    suspend fun list(): Response<HistoryListDto>

    @DELETE("history/{stationId}")
    suspend fun remove(
        @Path("stationId") stationId: String,
    ): Response<OkResult>

    @DELETE("history")
    suspend fun clear(): Response<OkResult>
}
