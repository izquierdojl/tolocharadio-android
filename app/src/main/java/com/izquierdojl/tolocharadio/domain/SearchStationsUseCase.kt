package com.izquierdojl.tolocharadio.domain

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.dto.StationPageDto
import com.izquierdojl.tolocharadio.data.repo.StationQuery
import com.izquierdojl.tolocharadio.data.repo.StationsRepo
import javax.inject.Inject

/** Búsqueda de emisoras con filtros y paginación (US-4). */
class SearchStationsUseCase
    @Inject
    constructor(private val repo: StationsRepo) {
        suspend operator fun invoke(query: StationQuery): ApiResult<StationPageDto> = repo.search(query)
    }

