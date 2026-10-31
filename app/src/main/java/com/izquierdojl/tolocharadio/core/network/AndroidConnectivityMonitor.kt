package com.izquierdojl.tolocharadio.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.izquierdojl.tolocharadio.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación Android de [ConnectivityMonitor] (spec 0042, R5).
 *
 * Usa `NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED` para no considerar
 * conectada una Wi-Fi sin salida real a Internet.
 */
@Singleton
class AndroidConnectivityMonitor
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ) : ConnectivityMonitor {
        private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        private val _isOnline = MutableStateFlow(isCurrentlyOnline())
        override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

        init {
            scope.launch {
                callbackFlow {
                    val callback =
                        object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: Network) {
                                trySend(isCurrentlyOnline())
                            }

                            override fun onLost(network: Network) {
                                trySend(isCurrentlyOnline())
                            }

                            override fun onCapabilitiesChanged(
                                network: Network,
                                networkCapabilities: NetworkCapabilities,
                            ) {
                                trySend(networkCapabilities.hasInternet())
                            }
                        }
                    manager.registerDefaultNetworkCallback(callback)
                    awaitClose { manager.unregisterNetworkCallback(callback) }
                }.distinctUntilChanged().collect { _isOnline.value = it }
            }
        }

        override suspend fun awaitOnline() {
            isOnline.first { it }
        }

        private fun isCurrentlyOnline(): Boolean {
            val network = manager.activeNetwork ?: return false
            val caps = manager.getNetworkCapabilities(network) ?: return false
            return caps.hasInternet()
        }

        private fun NetworkCapabilities.hasInternet(): Boolean =
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
