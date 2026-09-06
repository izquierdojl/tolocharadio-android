package com.izquierdojl.tolocharadio.domain

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import javax.inject.Inject

/**
 * Añade o quita un favorito. La UI actualiza de forma optimista y
 * revierte ante error (spec US-4).
 */
class ToggleFavoriteUseCase
    @Inject
    constructor(private val repo: FavoritesRepo) {
        /** @param isFavorite estado actual; devuelve el nuevo estado en éxito. */
        suspend operator fun invoke(
            stationId: String,
            isFavorite: Boolean,
        ): ApiResult<Boolean> {
            val r = if (isFavorite) repo.remove(stationId) else repo.add(stationId)
            return when (r) {
                is ApiResult.Ok -> ApiResult.Ok(!isFavorite)
                is ApiResult.Err -> r
            }
        }
    }
