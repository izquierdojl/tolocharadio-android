package com.izquierdojl.tolocharadio.domain.servers

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupGateTest {
    @Test
    fun `sin servidores el arranque pide el formulario de alta`() {
        assertEquals(StartupGate.NoServers, startupGateFor(0, activeHasCredentials = false))
    }

    @Test
    fun `servidor sin credenciales pide completarlas`() {
        assertEquals(StartupGate.NeedsCredentials, startupGateFor(1, activeHasCredentials = false))
    }

    @Test
    fun `servidor con credenciales esta listo`() {
        assertEquals(StartupGate.Ready, startupGateFor(1, activeHasCredentials = true))
        assertEquals(StartupGate.Ready, startupGateFor(3, activeHasCredentials = true))
    }
}
