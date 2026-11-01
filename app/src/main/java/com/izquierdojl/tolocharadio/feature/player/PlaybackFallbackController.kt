package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ejecutor del cambio único a la ruta de reserva (spec 0044, FR-003/FR-004).
 *
 * La decisión de ruta vive en [com.izquierdojl.tolocharadio.domain.playback.PlaybackRoutePolicy];
 * este controlador solo arranca la ruta legacy y permite testear el fallback sin
 * acoplar la política al player.
 */
@Singleton
class PlaybackFallbackController
    @Inject
    constructor(
        private val starter: LocalPlaybackStarter,
    ) {
        /** Arranca una conexión en [route]; devuelve fallo si el starter lanza. */
        suspend fun start(
            station: StationDto,
            route: PlaybackRoute,
            sequence: Long,
            recordHistory: Boolean = false,
        ): Result<Unit> =
            runCatching {
                starter.start(
                    station = station,
                    route = route,
                    sequence = sequence,
                    recordHistory = recordHistory,
                )
            }
    }
