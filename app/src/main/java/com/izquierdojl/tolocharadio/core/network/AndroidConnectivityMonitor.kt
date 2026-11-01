package com.izquierdojl.tolocharadio.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.izquierdojl.tolocharadio.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación Android de [ConnectivityMonitor] (spec 0042/0044).
 *
 * Usa `NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED` para no considerar
 * conectada una Wi-Fi sin salida real a Internet. [networkChanges] no emite en
 * el registro inicial y solo lo hace cuando cambia la red o el transporte.
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

        private val _networkChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val networkChanges: Flow<Unit> = _networkChanges.asSharedFlow()

        init {
            scope.launch {
                callbackFlow {
                    var initialNetworkSeen = false
                    var lastNetwork: Network? = null
                    var lastTransports: Set<Int> = emptySet()

                    fun emitNetworkChangeIfNeeded(
                        network: Network,
                        capabilities: NetworkCapabilities?,
                    ) {
                        val transports = capabilities?.transportSet() ?: emptySet()
                        if (initialNetworkSeen &&
                            (network != lastNetwork || transports != lastTransports)
                        ) {
                            _networkChanges.tryEmit(Unit)
                        }
                        initialNetworkSeen = true
                        lastNetwork = network
                        lastTransports = transports
                    }

                    val callback =
                        object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: Network) {
                                emitNetworkChangeIfNeeded(network, manager.getNetworkCapabilities(network))
                                trySend(isCurrentlyOnline())
                            }

                            override fun onLost(network: Network) {
                                if (network == lastNetwork) {
                                    lastNetwork = null
                                    lastTransports = emptySet()
                                }
                                trySend(isCurrentlyOnline())
                            }

                            override fun onCapabilitiesChanged(
                                network: Network,
                                networkCapabilities: NetworkCapabilities,
                            ) {
                                emitNetworkChangeIfNeeded(network, networkCapabilities)
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

        private fun NetworkCapabilities.transportSet(): Set<Int> =
            buildSet {
                if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add(NetworkCapabilities.TRANSPORT_WIFI)
                if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    add(NetworkCapabilities.TRANSPORT_CELLULAR)
                }
                if (hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    add(NetworkCapabilities.TRANSPORT_ETHERNET)
                }
                if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add(NetworkCapabilities.TRANSPORT_VPN)
            }
    }
