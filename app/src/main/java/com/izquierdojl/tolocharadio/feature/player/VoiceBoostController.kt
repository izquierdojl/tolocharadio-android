package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.di.ApplicationScope
import com.izquierdojl.tolocharadio.domain.podcasts.VoiceBoostPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado compartido del potenciador de voz (spec 0047, US4):
 * preferencia persistida, episodio local activo y sesión Cast.
 */
@Singleton
class VoiceBoostController
    @Inject
    constructor(
        private val prefs: InstancePrefs,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val _enabled = MutableStateFlow(false)
        val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

        private val episodeActive = AtomicBoolean(false)
        private val castConnected = AtomicBoolean(false)

        init {
            scope.launch {
                prefs.voiceBoost.collect { _enabled.value = it }
            }
        }

        /** Cambia la preferencia y la persiste sin bloquear la UI. */
        fun setEnabled(enabled: Boolean) {
            _enabled.value = enabled
            scope.launch { prefs.setVoiceBoost(enabled) }
        }

        /** Marca si hay un episodio local cargado. */
        fun setEpisodeActive(active: Boolean) {
            episodeActive.set(active)
        }

        /** Marca si la sesión remota de Cast está conectada. */
        fun setCastConnected(connected: Boolean) {
            castConnected.set(connected)
        }

        /** `true` cuando el DSP debe procesar el audio actual. */
        fun isEffective(): Boolean =
            VoiceBoostPolicy.isEffective(
                preferenceEnabled = _enabled.value,
                episodeActive = episodeActive.get(),
                castConnected = castConnected.get(),
            )
    }
