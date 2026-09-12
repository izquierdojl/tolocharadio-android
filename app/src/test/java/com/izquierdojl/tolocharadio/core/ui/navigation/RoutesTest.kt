package com.izquierdojl.tolocharadio.core.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun `detalle construye ruta con id`() {
        assertEquals("station/uuid-1", Routes.stationDetail("uuid-1"))
    }

    @Test
    fun `rutas de contenido`() {
        assertEquals("explore", Routes.EXPLORE)
        assertEquals("favorites", Routes.FAVORITES)
        assertEquals("history", Routes.HISTORY)
        assertEquals("custom-stations", Routes.CUSTOM_STATIONS)
        assertEquals("settings", Routes.SETTINGS)
        assertEquals("servers", Routes.SERVERS)
    }
}
