package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puente entre Configuración y el servicio para aplicar un perfil de buffer
 * (spec 0042, FR-013). El servicio marca [applying] mientras reconecta.
 */
@Singleton
class PlaybackProfileController
    @Inject
    constructor() {
        private val _requests = MutableSharedFlow<BufferProfile>(extraBufferCapacity = 1)
        val requests: SharedFlow<BufferProfile> = _requests.asSharedFlow()

        private val _applying = MutableStateFlow(false)
        val applying: StateFlow<Boolean> = _applying.asStateFlow()

        fun request(profile: BufferProfile) {
            _requests.tryEmit(profile)
        }

        fun setApplying(applying: Boolean) {
            _applying.value = applying
        }
    }
