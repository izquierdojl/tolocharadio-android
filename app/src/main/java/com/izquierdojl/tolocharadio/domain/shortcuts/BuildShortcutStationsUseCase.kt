package com.izquierdojl.tolocharadio.domain.shortcuts

import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import javax.inject.Inject

/**
 * Construye la lista de emisoras a publicar como accesos directos (FR-001/FR-003).
 *
 * Deduplica por `station.id` conservando la reproducción más reciente, ordena
 * por recencia descendente, recorta a [maxSlots] y asigna `rank` 0..N-1.
 * No confía en el orden de entrada (independiente de las invariantes del repo).
 *
 * Caso de uso puro del domain layer (constitución I): sin dependencias Android.
 */
class BuildShortcutStationsUseCase
    @Inject
    constructor() {
        operator fun invoke(
            entries: List<HistoryEntryDto>,
            maxSlots: Int,
        ): List<ShortcutStation> {
            if (maxSlots <= 0) return emptyList()
            return entries
                .filter { it.station.id.isNotBlank() }
                .groupBy { it.station.id }
                .mapNotNull { (_, group) -> group.maxByOrNull { it.playedAt } }
                .sortedByDescending { it.playedAt }
                .take(maxSlots)
                .mapIndexed { index, entry ->
                    ShortcutStation(
                        stationId = entry.station.id,
                        name = entry.station.name.ifBlank { FALLBACK_NAME },
                        faviconUrl = entry.station.favicon,
                        rank = index,
                    )
                }
        }

        private companion object {
            const val FALLBACK_NAME = "Emisora"
        }
    }
