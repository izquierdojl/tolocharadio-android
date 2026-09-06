package com.izquierdojl.tolocharadio.domain

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.repo.CustomStationsRepo
import com.izquierdojl.tolocharadio.data.repo.CustomStationsResult
import javax.inject.Inject

/**
 * Observa la lista de emisoras personalizadas (red + fallback a caché).
 */
class ObserveCustomStationsUseCase
    @Inject
    constructor(private val repo: CustomStationsRepo) {
        suspend operator fun invoke(): ApiResult<CustomStationsResult> = repo.list()
    }
