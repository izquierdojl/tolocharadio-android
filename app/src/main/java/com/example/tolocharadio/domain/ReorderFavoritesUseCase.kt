package com.example.tolocharadio.domain

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.DomainError
import com.example.tolocharadio.data.repo.FavoritesRepo
import javax.inject.Inject

/**
 * Guarda el orden personalizado. El servidor exige la permutación
 * exacta de ids: cualquier conjunto distinto, duplicado o con ids
 * en blanco se aborta localmente sin llamar a la API (VR-02/VR-03).
 */
class ReorderFavoritesUseCase
    @Inject
    constructor(private val repo: FavoritesRepo) {
        suspend operator fun invoke(
            current: List<String>,
            next: List<String>,
        ): ApiResult<Unit> {
            if (!isExactPermutation(current, next)) {
                return ApiResult.Err(DomainError.Validation(emptyList()))
            }
            return repo.reorder(next)
        }

        private fun isExactPermutation(
            current: List<String>,
            next: List<String>,
        ): Boolean {
            if (next.size != current.size || next.any { it.isBlank() }) return false
            val nextSet = next.toSet()
            return nextSet.size == next.size && nextSet == current.toSet()
        }
    }
