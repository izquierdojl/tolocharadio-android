package com.izquierdojl.tolocharadio.feature.shortcuts

import androidx.lifecycle.ViewModel
import com.izquierdojl.tolocharadio.core.shortcuts.PendingShortcut
import com.izquierdojl.tolocharadio.core.shortcuts.PendingShortcutHolder
import com.izquierdojl.tolocharadio.domain.shortcuts.ResolveShortcutLaunchUseCase
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutLaunchResolution
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Expone el acceso directo pendiente y resuelve su lanzamiento. El grafo
 * de navegación consume el resultado para reproducir o avisar (FR-005/007).
 */
@HiltViewModel
class ShortcutLaunchViewModel
    @Inject
    constructor(
        private val pendingHolder: PendingShortcutHolder,
        private val resolveLaunch: ResolveShortcutLaunchUseCase,
    ) : ViewModel() {
        val pending: StateFlow<PendingShortcut?> = pendingHolder.pending

        suspend fun resolve(stationId: String): ShortcutLaunchResolution = resolveLaunch(stationId)

        fun consume() = pendingHolder.clear()
    }
