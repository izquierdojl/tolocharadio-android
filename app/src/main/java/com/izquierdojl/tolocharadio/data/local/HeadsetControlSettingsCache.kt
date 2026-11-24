package com.izquierdojl.tolocharadio.data.local

import com.izquierdojl.tolocharadio.di.ApplicationScope
import com.izquierdojl.tolocharadio.domain.playback.HeadsetControlSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caché en memoria de [HeadsetControlSettings] (spec 0060).
 *
 * El servicio, el controlador y el gesto de volumen necesitan el valor actual
 * sin suspender; [settings] se comparte como `StateFlow` y arranca con
 * [HeadsetControlSettings.DEFAULT].
 */
@Singleton
class HeadsetControlSettingsCache
    @Inject
    constructor(
        prefs: InstancePrefs,
        @ApplicationScope scope: CoroutineScope,
    ) {
        /** Ajustes vigentes; se actualizan con cada emisión de DataStore. */
        val settings: StateFlow<HeadsetControlSettings> =
            prefs.headsetControlSettings.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = HeadsetControlSettings.DEFAULT,
            )

        /** Valor actual sin suspender. */
        fun current(): HeadsetControlSettings = settings.value
    }
