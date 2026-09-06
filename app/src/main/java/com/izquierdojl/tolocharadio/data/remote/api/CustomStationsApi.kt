package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.CreateCustomStationBody
import com.izquierdojl.tolocharadio.data.remote.dto.CustomStationResultDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationListDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Emisoras personalizadas del usuario (Bearer). Paridad con
 * `CustomStations.tsx` web (`GET/POST /custom-stations`,
 * `DELETE /custom-stations/:id`).
 */
interface CustomStationsApi {
    /** Lista las emisoras personalizadas del usuario (`{items}`). */
    @GET("custom-stations")
    suspend fun list(): Response<StationListDto>

    /**
     * Crea una emisora personalizada (`{name 1–256, url http(s)}` →
     * `{station}`). El servidor responde 422 con `details` por campo
     * si la validación falla.
     */
    @POST("custom-stations")
    suspend fun create(
        @Body body: CreateCustomStationBody,
    ): Response<CustomStationResultDto>

    /** Elimina una emisora personalizada por id. */
    @DELETE("custom-stations/{id}")
    suspend fun delete(
        @Path("id") id: String,
    ): Response<OkResult>
}

