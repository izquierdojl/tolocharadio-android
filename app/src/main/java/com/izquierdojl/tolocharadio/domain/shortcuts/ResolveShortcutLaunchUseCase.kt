package com.izquierdojl.tolocharadio.domain.shortcuts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.data.repo.StationsRepo
import javax.inject.Inject

/**
 * Decide qué hacer al pulsar un acceso directo (FR-005/006/007/011).
 *
 * Busca la emisora en el historial en memoria, luego en la caché y, si no,
 * la recupera del servidor; si nada funciona → [ShortcutLaunchResolution.Unavailable].
 * La disponibilidad de reproducción se comprueba en `PlayerViewModel.play`.
 */
class ResolveShortcutLaunchUseCase
    @Inject
    constructor(
        private val historyRepo: HistoryRepo,
        private val stationsRepo: StationsRepo,
    ) {
        suspend operator fun invoke(stationId: String): ShortcutLaunchResolution {
            if (stationId.isBlank()) return ShortcutLaunchResolution.Unavailable(NOT_AVAILABLE)
            historyRepo.items.value.firstOrNull { it.station.id == stationId }?.let {
                return ShortcutLaunchResolution.Play(it.station)
            }
            historyRepo.snapshot().firstOrNull { it.station.id == stationId }?.let {
                return ShortcutLaunchResolution.Play(it.station)
            }
            return when (val result = stationsRepo.detail(stationId)) {
                is ApiResult.Ok -> ShortcutLaunchResolution.Play(result.value)
                is ApiResult.Err -> ShortcutLaunchResolution.Unavailable(result.error.userMessage())
            }
        }

        private companion object {
            const val NOT_AVAILABLE = "Emisora no disponible."
        }
    }
