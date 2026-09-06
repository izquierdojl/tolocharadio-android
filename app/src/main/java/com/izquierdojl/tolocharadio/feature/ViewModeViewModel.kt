package com.izquierdojl.tolocharadio.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fuente única del modo de vista lista/tarjetas (spec 008).
 *
 * Se instancia a ámbito de Activity (como `PlayerViewModel`) para que la
 * TopAppBar compartida y las 4 secciones observen el mismo [mode] (FR-004).
 * `toggle()` emite el nuevo valor de forma optimista y lo persiste en
 * DataStore sin tocar datos de red (FR-003, FR-006).
 */
@HiltViewModel
class ViewModeViewModel
    @Inject
    constructor(
        private val prefs: InstancePrefs,
    ) : ViewModel() {
        private val _mode = MutableStateFlow(ViewMode.LIST)

        /** Modo activo; inicia con la preferencia persistida (default LIST, FR-005). */
        val mode: StateFlow<ViewMode> = _mode.asStateFlow()

        init {
            viewModelScope.launch {
                prefs.viewMode.collect { persisted -> _mode.value = persisted }
            }
        }

        /** Alterna LIST⇄GRID, emite de inmediato y persiste la elección. */
        fun toggle() {
            val next = if (_mode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            _mode.value = next
            viewModelScope.launch { prefs.setViewMode(next) }
        }
    }
