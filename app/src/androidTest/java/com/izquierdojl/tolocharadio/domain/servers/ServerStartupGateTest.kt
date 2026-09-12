package com.izquierdojl.tolocharadio.domain.servers

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verificación instrumentada del gate de arranque: sin servidores →
 * formulario; servidor sin credenciales → completarlas; con
 * credenciales → contenido (FR-010/FR-011).
 */
@RunWith(AndroidJUnit4::class)
class ServerStartupGateTest {
    @Test
    fun sin_servidores_pide_alta() {
        assertEquals(StartupGate.NoServers, startupGateFor(0, activeHasCredentials = false))
    }

    @Test
    fun servidor_sin_credenciales_pide_completarlas() {
        assertEquals(StartupGate.NeedsCredentials, startupGateFor(1, activeHasCredentials = false))
    }

    @Test
    fun servidor_con_credenciales_esta_listo() {
        assertEquals(StartupGate.Ready, startupGateFor(1, activeHasCredentials = true))
    }
}
