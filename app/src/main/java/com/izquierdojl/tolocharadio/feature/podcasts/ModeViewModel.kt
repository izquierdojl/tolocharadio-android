package com.izquierdojl.tolocharadio.feature.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.ui.navigation.ContentMode
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.repo.SystemRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado del modo global Radio|Podcasts (spec 0046, US1). */
data class ModeUi(
    val mode: ContentMode = ContentMode.RADIO,
    val podcastsEnabled: Boolean = false,
    val defaultSection: ContentMode = ContentMode.RADIO,
    val loading: Boolean = true,
)

/**
 * Modo global de contenido. Lee las capacidades del servidor activo y la
 * preferencia; el cambio de modo persiste sin tocar la reproducción.
 */
@HiltViewModel
class ModeViewModel
    @Inject
    constructor(
        private val prefs: InstancePrefs,
        private val system: SystemRepo,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(ModeUi())
        val ui: StateFlow<ModeUi> = _ui.asStateFlow()

        init {
            viewModelScope.launch {
                prefs.contentMode.collect { mode ->
                    _ui.value = _ui.value.copy(mode = mode)
                }
            }
            viewModelScope.launch {
                prefs.defaultSection.collect { section ->
                    _ui.value = _ui.value.copy(defaultSection = section)
                }
            }
            refresh()
        }

        /** Recarga las capacidades del servidor activo (puerta del modo). */
        fun refresh() {
            viewModelScope.launch {
                val enabled =
                    when (val r = system.podcastCapabilities()) {
                        is ApiResult.Ok -> r.value.podcasts
                        is ApiResult.Err -> false
                    }
                _ui.value = _ui.value.copy(podcastsEnabled = enabled, loading = false)
            }
        }

        /** Cambia de modo Radio↔Podcasts (FR-002). */
        fun setMode(mode: ContentMode) {
            _ui.value = _ui.value.copy(mode = mode)
            viewModelScope.launch { prefs.setContentMode(mode) }
        }
    }
