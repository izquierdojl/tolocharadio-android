package com.izquierdojl.tolocharadio.core.ui.navigation

import com.izquierdojl.tolocharadio.domain.servers.StartupGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Puerta del arranque: una sola aplicación, con capacidades ya resueltas. */
class StartDestinationGateTest {
    @Test
    fun `aplica el arranque solo en la ruta inicial y sin capacidades pendientes`() {
        assertTrue(
            shouldApplyStartDestination(
                currentRoute = Routes.HOME,
                startupGate = StartupGate.Ready,
                modeLoading = false,
                alreadyApplied = false,
            ),
        )
        assertFalse(
            shouldApplyStartDestination(
                currentRoute = Routes.HOME,
                startupGate = StartupGate.Ready,
                modeLoading = true,
                alreadyApplied = false,
            ),
        )
        assertFalse(
            shouldApplyStartDestination(
                currentRoute = Routes.HOME,
                startupGate = StartupGate.Ready,
                modeLoading = false,
                alreadyApplied = true,
            ),
        )
        assertFalse(
            shouldApplyStartDestination(
                currentRoute = Routes.HOME,
                startupGate = StartupGate.NoServers,
                modeLoading = false,
                alreadyApplied = false,
            ),
        )
        assertFalse(
            shouldApplyStartDestination(
                currentRoute = Routes.EXPLORE,
                startupGate = StartupGate.Ready,
                modeLoading = false,
                alreadyApplied = false,
            ),
        )
    }
}
