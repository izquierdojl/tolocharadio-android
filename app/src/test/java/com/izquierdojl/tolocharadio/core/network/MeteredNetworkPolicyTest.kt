package com.izquierdojl.tolocharadio.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Política pura de red medida (spec 0058, FR-017). */
class MeteredNetworkPolicyTest {
    @Test
    fun `wifi sin ahorro de datos no es medida`() {
        assertFalse(
            MeteredNetworkPolicy.isMetered(
                NetworkSignals(notMettered = true, notRoaming = true, legacyRoaming = false, dataSaver = false),
            ),
        )
    }

    @Test
    fun `movil sin NOT_METERED es medida`() {
        assertTrue(
            MeteredNetworkPolicy.isMetered(
                NetworkSignals(notMettered = false, notRoaming = true, legacyRoaming = false, dataSaver = false),
            ),
        )
    }

    @Test
    fun `roaming en API 28 es medida`() {
        assertTrue(
            MeteredNetworkPolicy.isMetered(
                NetworkSignals(notMettered = true, notRoaming = false, legacyRoaming = false, dataSaver = false),
            ),
        )
    }

    @Test
    fun `roaming legacy en API 26 es medida`() {
        assertTrue(
            MeteredNetworkPolicy.isMetered(
                NetworkSignals(notMettered = true, notRoaming = null, legacyRoaming = true, dataSaver = false),
            ),
        )
    }

    @Test
    fun `el ahorro de datos del sistema es medida`() {
        assertTrue(
            MeteredNetworkPolicy.isMetered(
                NetworkSignals(notMettered = true, notRoaming = true, legacyRoaming = false, dataSaver = true),
            ),
        )
    }
}
