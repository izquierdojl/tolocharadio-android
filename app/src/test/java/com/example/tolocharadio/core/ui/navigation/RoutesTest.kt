package com.example.tolocharadio.core.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun `detalle construye ruta con id`() {
        assertEquals("station/uuid-1", Routes.stationDetail("uuid-1"))
    }

    @Test
    fun `destinos protegidos exigen auth`() {
        assertEquals(true, Routes.EXPLORE in AUTH_REQUIRED)
        assertEquals(false, Routes.HOME in AUTH_REQUIRED)
        assertEquals(false, Routes.LOGIN in AUTH_REQUIRED)
    }

    @Test
    fun `favoritos exige auth con paridad web`() {
        assertEquals(true, Routes.FAVORITES in AUTH_REQUIRED)
        assertEquals("favorites", Routes.FAVORITES)
    }
}
