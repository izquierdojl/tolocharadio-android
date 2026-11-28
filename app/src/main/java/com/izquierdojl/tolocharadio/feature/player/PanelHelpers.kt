package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto

/**
 * Enlace a copiar al portapapeles (spec 004, FR-006): la URL original
 * del stream, jamás la del proxy autenticado. Sin URL → `null` (la UI
 * muestra "enlace no disponible" y no copia nada).
 */
fun resolveCopyLink(station: StationDto): String? = station.url.trim().ifEmpty { null }
