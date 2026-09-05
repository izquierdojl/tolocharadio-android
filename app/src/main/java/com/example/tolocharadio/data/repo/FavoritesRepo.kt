package com.example.tolocharadio.data.repo

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.safeCall
import com.example.tolocharadio.data.remote.api.AddFavoriteBody
import com.example.tolocharadio.data.remote.api.FavoritesApi
import com.example.tolocharadio.data.remote.dto.FavoriteDto
import javax.inject.Inject
import javax.inject.Singleton

/** Toggle de favoritos (lista completa → siguiente spec). */
@Singleton
class FavoritesRepo
    @Inject
    constructor(private val api: FavoritesApi) {
        /** Añade un favorito por `stationId`. */
        suspend fun add(stationId: String): ApiResult<FavoriteDto> =
            when (val r = safeCall { api.add(AddFavoriteBody(stationId)) }) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.favorite)
                is ApiResult.Err -> r
            }

        /** Quita un favorito (éxito aunque no existiera). */
        suspend fun remove(stationId: String): ApiResult<Unit> =
            when (val r = safeCall { api.remove(stationId) }) {
                is ApiResult.Ok -> ApiResult.Ok(Unit)
                is ApiResult.Err -> r
            }
    }
