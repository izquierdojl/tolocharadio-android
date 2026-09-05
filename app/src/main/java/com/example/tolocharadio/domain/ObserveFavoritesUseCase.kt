package com.example.tolocharadio.domain

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.data.repo.FavoritesRepo
import com.example.tolocharadio.data.repo.FavoritesResult
import javax.inject.Inject

/**
 * Observa la lista de favoritas (red + fallback a caché). Deduplica
 * por `station.id` conservando el primer orden del servidor (VR-01).
 */
class ObserveFavoritesUseCase
    @Inject
    constructor(private val repo: FavoritesRepo) {
        suspend operator fun invoke(): ApiResult<FavoritesResult> {
            return when (val r = repo.list()) {
                is ApiResult.Ok ->
                    ApiResult.Ok(
                        r.value.copy(items = r.value.items.distinctBy { it.station.id }),
                    )
                is ApiResult.Err -> r
            }
        }
    }
