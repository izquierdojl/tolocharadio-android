package com.izquierdojl.tolocharadio.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** Doble de test de [ConnectivityMonitor] (spec 0042, T002). */
class FakeConnectivityMonitor(
    initialOnline: Boolean = true,
) : ConnectivityMonitor {
    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val networkChanges: Flow<Unit> = _networkChanges.asSharedFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    fun emitNetworkChange() {
        _networkChanges.tryEmit(Unit)
    }

    override suspend fun awaitOnline() {
        isOnline.first { it }
    }
}
