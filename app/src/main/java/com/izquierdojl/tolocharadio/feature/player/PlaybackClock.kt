package com.izquierdojl.tolocharadio.feature.player

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/** Reloj inyectable del supervisor para probar ventanas temporales. */
fun interface PlaybackClock {
    fun nowMs(): Long
}

/** Implementación de producción basada en [SystemClock.elapsedRealtime]. */
@Singleton
class SystemPlaybackClock
    @Inject
    constructor() : PlaybackClock {
        override fun nowMs(): Long = SystemClock.elapsedRealtime()
    }
