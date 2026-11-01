package com.izquierdojl.tolocharadio.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado de conectividad del dispositivo (spec 0042/0044).
 *
 * Es una interfaz para poder sustituirlo en tests; la implementación Android
 * usa `registerDefaultNetworkCallback`.
 */
interface ConnectivityMonitor {
    /** `true` si hay una red con Internet validado. */
    val isOnline: StateFlow<Boolean>

    /** Emite cuando cambia la red o el transporte (Wi-Fi ↔ datos móviles). */
    val networkChanges: Flow<Unit>

    /** Suspende hasta que haya conectividad. */
    suspend fun awaitOnline()
}
