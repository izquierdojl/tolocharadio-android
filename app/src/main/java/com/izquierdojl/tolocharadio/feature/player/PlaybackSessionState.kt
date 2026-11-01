package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.domain.playback.PlaybackFeatureFlags
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado compartido de la sesión de reproducción (spec 0044).
 *
 * Centraliza la ruta efectiva (`CONTINUOUS`/`LEGACY`) y el marcador anticaché
 * monótono `?r=`. Lo usan el arranque inicial, la recuperación, el cambio de
 * perfil y la reanudación local tras Cast, de forma que dos conexiones nunca
 * comparten el mismo marcador y un fallback no se repite al recrear el player.
 */
@Singleton
class PlaybackSessionState
    @Inject
    constructor(flags: PlaybackFeatureFlags) {
        private val sequence = AtomicLong(0)

        @Volatile
        var route: PlaybackRoute = flags.defaultRoute
            private set

        /** Reinicia solo la ruta (el marcador sigue siendo monótono). */
        fun resetRoute(route: PlaybackRoute) {
            this.route = route
        }

        /** Cambia la ruta efectiva a la reserva; no hay vuelta atrás en la sesión. */
        fun useLegacyRoute() {
            route = PlaybackRoute.LEGACY
        }

        /** Siguiente marcador `?r=`; empieza en 0 y nunca se reutiliza. */
        fun nextSequence(): Long = sequence.getAndIncrement()
    }
