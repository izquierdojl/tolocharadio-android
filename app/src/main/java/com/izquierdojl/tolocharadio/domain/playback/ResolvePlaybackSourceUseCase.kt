package com.izquierdojl.tolocharadio.domain.playback

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import javax.inject.Inject

/**
 * Decide la fuente de reproducción de una emisora (spec 0021): **siempre** el
 * proxy autenticado, marcando si es HLS por la extensión de su enlace.
 *
 * Función pura respecto a Android y sin red: la resolución de listas y HLS la
 * hace el servicio. Nunca falla.
 */
class ResolvePlaybackSourceUseCase
    @Inject
    constructor() {
        /** Resuelve [station] a su fuente de reproducción por proxy. */
        operator fun invoke(station: StationDto): PlaybackSource =
            PlaybackSource(
                stationId = station.id,
                hls = HlsStation.isHls(station.url),
            )
    }
