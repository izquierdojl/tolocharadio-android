package com.izquierdojl.tolocharadio.domain.servers

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupGateTest {
    @Test
    fun `sin servidores el arranque requiere configuracion`() {
        assertEquals(StartupGate.NoServers, startupGateFor(0))
    }

    @Test
    fun `con un servidor el arranque esta listo`() {
        assertEquals(StartupGate.Ready, startupGateFor(1))
        assertEquals(StartupGate.Ready, startupGateFor(3))
    }
}
