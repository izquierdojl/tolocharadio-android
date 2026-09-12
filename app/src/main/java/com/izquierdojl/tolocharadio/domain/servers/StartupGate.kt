package com.izquierdojl.tolocharadio.domain.servers

/** Destino de arranque según los servidores configurados (FR-002). */
sealed interface StartupGate {
    /** No hay ningún servidor: pantalla de bienvenida bloqueante. */
    data object NoServers : StartupGate

    /** Hay al menos un servidor: se puede acceder al contenido. */
    data object Ready : StartupGate
}

/** Deriva el destino de arranque del número de servidores guardados. */
fun startupGateFor(serverCount: Int): StartupGate = if (serverCount > 0) StartupGate.Ready else StartupGate.NoServers
