package com.izquierdojl.tolocharadio.feature.shortcuts

import androidx.lifecycle.ViewModel
import com.izquierdojl.tolocharadio.core.session.AuthState
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.shortcuts.PendingShortcut
import com.izquierdojl.tolocharadio.core.shortcuts.PendingShortcutHolder
import com.izquierdojl.tolocharadio.domain.shortcuts.ResolveShortcutLaunchUseCase
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutLaunchResolution
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Expone el acceso directo pendiente y resuelve su lanzamiento. El grafo
 * de navegación consume el resultado para reproducir/navegar (FR-005/007).
 */
@HiltViewModel
class ShortcutLaunchViewModel
    @Inject
    constructor(
        private val pendingHolder: PendingShortcutHolder,
        private val resolveLaunch: ResolveShortcutLaunchUseCase,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        val pending: StateFlow<PendingShortcut?> = pendingHolder.pending

        val authState: StateFlow<AuthState> = sessionManager.authState

        suspend fun resolve(stationId: String): ShortcutLaunchResolution =
            resolveLaunch(stationId, sessionManager.authState.value)

        fun consume() = pendingHolder.clear()
    }
