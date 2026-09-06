package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationPageDto
import com.izquierdojl.tolocharadio.data.remote.dto.StringListDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Catálogo público con paginación `limit/offset/hasMore`. */
interface StationsApi {
    @GET("stations")
    suspend fun search(
        @Query("name") name: String? = null,
        @Query("country") country: String? = null,
        @Query("language") language: String? = null,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = 24,
        @Query("offset") offset: Int = 0,
        @Query("unique") unique: Boolean = false,
    ): Response<StationPageDto>

    @GET("stations/{id}")
    suspend fun detail(
        @Path("id") id: String,
    ): Response<StationDto>

    @GET("stations/countries")
    suspend fun countries(): Response<StringListDto>

    @GET("stations/languages")
    suspend fun languages(): Response<StringListDto>

    @GET("stations/tags")
    suspend fun tags(): Response<StringListDto>
}

