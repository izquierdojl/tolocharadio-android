package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto

/**
 * Segunda línea del panel inferior (spec 004, FR-002):
 * `"{país} · {idioma} · {codec} {bitrate} kbps"`, omitiendo cada parte
 * ausente. Sin ningún dato devuelve `"Emisora de radio"`.
 */
fun panelSubtitle(station: StationDto): String {
    val quality =
        listOfNotNull(
            station.codec?.takeIf { it.isNotBlank() },
            station.bitrate?.takeIf { it > 0 }?.let { "$it kbps" },
        ).joinToString(" ").ifBlank { null }
    val parts =
        listOfNotNull(
            station.country?.takeIf { it.isNotBlank() },
            station.language?.takeIf { it.isNotBlank() },
            quality,
        )
    return if (parts.isEmpty()) "Emisora de radio" else parts.joinToString(" · ")
}

/**
 * Enlace a copiar al portapapeles (spec 004, FR-006): la URL original
 * del stream, jamás la del proxy autenticado. Sin URL → `null` (la UI
 * muestra "enlace no disponible" y no copia nada).
 */
fun resolveCopyLink(station: StationDto): String? = station.url.trim().ifEmpty { null }

