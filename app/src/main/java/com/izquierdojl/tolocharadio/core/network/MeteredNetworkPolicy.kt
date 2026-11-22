package com.izquierdojl.tolocharadio.core.network

/** Señales de la red activa para decidir si es medida (spec 0058, R7/FR-017). */
data class NetworkSignals(
    /** `NET_CAPABILITY_NOT_METERED`. */
    val notMettered: Boolean,
    /** `NET_CAPABILITY_NOT_ROAMING`; `null` en API < 28. */
    val notRoaming: Boolean?,
    /** `TelephonyManager.isNetworkRoaming` para API < 28. */
    val legacyRoaming: Boolean,
    /** Ahorro de datos del sistema activo. */
    val dataSaver: Boolean,
)

/**
 * Política pura de red medida: móvil, roaming o Ahorro de datos.
 *
 * Se extrae de `AndroidConnectivityMonitor` para poder testear las
 * combinaciones de capacidades sin Android (FR-017).
 */
object MeteredNetworkPolicy {
    fun isMetered(signals: NetworkSignals): Boolean =
        !signals.notMettered ||
            signals.notRoaming == false ||
            (signals.notRoaming == null && signals.legacyRoaming) ||
            signals.dataSaver
}
