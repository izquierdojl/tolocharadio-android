package com.izquierdojl.tolocharadio.core.shortcuts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Petición de reproducción pendiente desde un acceso directo (transitoria). */
data class PendingShortcut(
    val stationId: String,
    val stationName: String? = null,
)

/**
 * Puente en memoria entre `MainActivity` (recibe el intent) y el grafo de
 * navegación (donde vive el `PlayerViewModel` activity-scoped). Un único
 * valor: la última pulsación prevalece (contrato de idempotencia).
 */
@Singleton
class PendingShortcutHolder
    @Inject
    constructor() {
        private val _pending = MutableStateFlow<PendingShortcut?>(null)

        val pending: StateFlow<PendingShortcut?> = _pending.asStateFlow()

        fun set(shortcut: PendingShortcut) {
            _pending.value = shortcut
        }

        fun clear() {
            _pending.value = null
        }
    }
