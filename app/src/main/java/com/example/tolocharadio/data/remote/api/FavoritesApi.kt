package com.example.tolocharadio.data.remote.api

import com.example.tolocharadio.data.remote.dto.FavoriteListDto
import com.example.tolocharadio.data.remote.dto.FavoriteResultDto
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class AddFavoriteBody(val stationId: String)

/** Favoritos (Bearer). Lista completa y orden → siguiente spec (FR-009). */
interface FavoritesApi {
    @GET("favorites")
    suspend fun list(): Response<FavoriteListDto>

    @POST("favorites")
    suspend fun add(
        @Body body: AddFavoriteBody,
    ): Response<FavoriteResultDto>

    @DELETE("favorites/{stationId}")
    suspend fun remove(
        @Path("stationId") stationId: String,
    ): Response<OkResult>
}
