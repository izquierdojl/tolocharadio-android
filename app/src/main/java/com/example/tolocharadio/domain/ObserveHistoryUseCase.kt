package com.example.tolocharadio.domain

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.data.repo.HistoryRepo
import com.example.tolocharadio.data.repo.HistoryResult
import javax.inject.Inject

/**
 * Observa la lista de historial (red + fallback a caché). Deduplica
 * por `station.id` conservando la reproducción más reciente por emisora.
 */
class ObserveHistoryUseCase
    @Inject
    constructor(private val repo: HistoryRepo) {
        suspend operator fun invoke(): ApiResult<HistoryResult> = repo.list()
    }
