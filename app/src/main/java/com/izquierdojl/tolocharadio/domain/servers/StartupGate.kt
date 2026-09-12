package com.izquierdojl.tolocharadio.domain.servers

/** Destino de arranque según los servidores y sus credenciales (FR-010/FR-011). */
sealed interface StartupGate {
    /** No hay ningún servidor: pantalla unificada de alta (bloqueante). */
    data object NoServers : StartupGate

    /** El servidor de arranque no tiene credenciales: edición bloqueante. */
    data object NeedsCredentials : StartupGate

    /** Hay servidor con credenciales: sesión automática y contenido. */
    data object Ready : StartupGate
}

/** Deriva el destino de arranque del número de servidores y sus credenciales. */
fun startupGateFor(
    serverCount: Int,
    activeHasCredentials: Boolean,
): StartupGate =
    when {
        serverCount <= 0 -> StartupGate.NoServers
        !activeHasCredentials -> StartupGate.NeedsCredentials
        else -> StartupGate.Ready
    }
