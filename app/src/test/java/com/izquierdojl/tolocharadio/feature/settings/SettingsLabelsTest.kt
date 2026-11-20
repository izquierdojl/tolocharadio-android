package com.izquierdojl.tolocharadio.feature.settings

import com.izquierdojl.tolocharadio.core.ui.navigation.PodcastStartScreen
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

    @Test
    fun `etiquetas de pantalla de arranque de podcast`() {
        assertEquals("Inicio", PodcastStartScreen.HOME.label())
        assertEquals("Descubrir", PodcastStartScreen.DISCOVER.label())
        assertEquals("Suscripciones", PodcastStartScreen.SUBSCRIPTIONS.label())
        assertEquals("Cola", PodcastStartScreen.QUEUE.label())
        assertEquals("Favoritos", PodcastStartScreen.FAVORITES.label())
    }
}
