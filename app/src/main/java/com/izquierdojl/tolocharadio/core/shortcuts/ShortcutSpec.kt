package com.izquierdojl.tolocharadio.core.shortcuts

import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutStation
import javax.inject.Inject

/**
 * Representación neutral de un acceso directo antes de tocar la API Android.
 * Mantiene la lógica de id/etiquetas/rank fuera del wrapper de plataforma
 * para poder testearla en JVM (research.md R8).
 */
data class ShortcutSpec(
    val id: String,
    val shortLabel: String,
    val longLabel: String,
    val iconUrl: String?,
    val intentAction: String,
    val intentStationId: String,
    val intentStationName: String,
    val rank: Int,
)

/**
 * Construye [ShortcutSpec] a partir de emisoras ya seleccionadas (FR-014).
 * El id es determinista (`hist-<stationId>`) para que republicar actualice
 * el mismo elemento en lugar de duplicarlo.
 */
class ShortcutSpecFactory
    @Inject
    constructor() {
        fun create(stations: List<ShortcutStation>): List<ShortcutSpec> =
            stations.map { station ->
                val name = station.name.ifBlank { FALLBACK_NAME }
                ShortcutSpec(
                    id = "hist-${station.stationId}",
                    shortLabel = truncate(name, SHORT_LABEL_MAX),
                    longLabel = truncate(name, LONG_LABEL_MAX),
                    iconUrl = station.faviconUrl,
                    intentAction = ShortcutIntents.ACTION_OPEN_STATION,
                    intentStationId = station.stationId,
                    intentStationName = name,
                    rank = station.rank,
                )
            }

        private fun truncate(
            value: String,
            max: Int,
        ): String =
            if (value.length <= max) {
                value
            } else {
                value.take((max - 1).coerceAtLeast(0)).trimEnd() + ELLIPSIS
            }

        private companion object {
            const val FALLBACK_NAME = "Emisora"
            const val SHORT_LABEL_MAX = 25
            const val LONG_LABEL_MAX = 60
            const val ELLIPSIS = "…"
        }
    }
