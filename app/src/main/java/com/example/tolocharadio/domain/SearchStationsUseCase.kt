package com.example.tolocharadio.domain

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.data.remote.dto.StationPageDto
import com.example.tolocharadio.data.repo.StationQuery
import com.example.tolocharadio.data.repo.StationsRepo
import javax.inject.Inject

/** Búsqueda de emisoras con filtros y paginación (US-4). */
class SearchStationsUseCase
    @Inject
    constructor(private val repo: StationsRepo) {
        suspend operator fun invoke(query: StationQuery): ApiResult<StationPageDto> = repo.search(query)
    }
