package com.izquierdojl.tolocharadio.feature.settings

import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLabelsTest {
    @Test
    fun `etiquetas de tema`() {
        assertEquals("Sistema", ThemeMode.SYSTEM.label())
        assertEquals("Oscuro", ThemeMode.DARK.label())
        assertEquals("Claro", ThemeMode.LIGHT.label())
    }

    @Test
    fun `etiquetas de pantalla de arranque`() {
        assertEquals("Explorar", StartScreen.EXPLORE.label())
        assertEquals("Favoritos", StartScreen.FAVORITES.label())
        assertEquals("Historial", StartScreen.HISTORY.label())
    }
}
