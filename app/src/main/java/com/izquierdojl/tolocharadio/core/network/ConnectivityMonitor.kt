package com.izquierdojl.tolocharadio.core.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Estado de conectividad del dispositivo (spec 0042, R5).
 *
 * Es una interfaz para poder sustituirlo en tests; la implementación Android
 * usa `registerDefaultNetworkCallback`.
 */
interface ConnectivityMonitor {
    /** `true` si hay una red con Internet validado. */
    val isOnline: StateFlow<Boolean>

    /** Suspende hasta que haya conectividad. */
    suspend fun awaitOnline()
}
