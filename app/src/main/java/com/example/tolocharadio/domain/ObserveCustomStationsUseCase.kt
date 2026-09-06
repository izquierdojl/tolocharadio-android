package com.example.tolocharadio.domain

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.data.repo.CustomStationsRepo
import com.example.tolocharadio.data.repo.CustomStationsResult
import javax.inject.Inject

/**
 * Observa la lista de emisoras personalizadas (red + fallback a caché).
 */
class ObserveCustomStationsUseCase
    @Inject
    constructor(private val repo: CustomStationsRepo) {
        suspend operator fun invoke(): ApiResult<CustomStationsResult> = repo.list()
    }
