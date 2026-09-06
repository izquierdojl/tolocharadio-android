package com.izquierdojl.tolocharadio.core.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T008: resolución del selector Sistema/Claro/Oscuro (FR-010). */
class ThemeModeTest {
    @Test fun `SYSTEM sigue al sistema`() {
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, systemDark = false))
    }

    @Test fun `DARK siempre oscuro`() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemDark = false))
    }

    @Test fun `LIGHT siempre claro`() {
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemDark = true))
    }
}

