package com.izquierdojl.tolocharadio.data.remote.api

import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteListDto
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteResultDto
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

@Serializable
data class AddFavoriteBody(val stationId: String)

/**
 * Orden personalizado: la lista COMPLETA de ids en el nuevo orden.
 * Confirmado contra `apps/api/src/routes/favorites.ts`:
 * `reorderSchema = z.object({ stationIds: z.array(...).min(1) })`.
 */
@Serializable
data class ReorderBody(val stationIds: List<String>)

/** Favoritos (Bearer). */
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

    @PUT("favorites/order")
    suspend fun reorder(
        @Body body: ReorderBody,
    ): Response<OkResult>
}

