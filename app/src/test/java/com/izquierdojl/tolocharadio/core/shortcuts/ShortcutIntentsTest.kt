package com.izquierdojl.tolocharadio.core.shortcuts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutIntentsTest {
    @Test
    fun `parsea accion y extras validos`() {
        val parsed = ShortcutIntents.parse(ShortcutIntents.ACTION_OPEN_STATION, "s1", "Rock FM")
        assertEquals("s1", parsed?.stationId)
        assertEquals("Rock FM", parsed?.stationName)
    }

    @Test
    fun `accion distinta devuelve null`() {
        assertNull(ShortcutIntents.parse("otra.accion", "s1", null))
        assertNull(ShortcutIntents.parse(null, "s1", null))
    }

    @Test
    fun `station id ausente o en blanco devuelve null`() {
        assertNull(ShortcutIntents.parse(ShortcutIntents.ACTION_OPEN_STATION, null, null))
        assertNull(ShortcutIntents.parse(ShortcutIntents.ACTION_OPEN_STATION, "   ", null))
    }

    @Test
    fun `recorta espacios y normaliza nombre vacio`() {
        val parsed = ShortcutIntents.parse(ShortcutIntents.ACTION_OPEN_STATION, "  s1  ", "   ")
        assertEquals("s1", parsed?.stationId)
        assertNull(parsed?.stationName)
    }

    @Test
    fun `nombre opcional se conserva`() {
        val parsed = ShortcutIntents.parse(ShortcutIntents.ACTION_OPEN_STATION, "s1", null)
        assertTrue(parsed != null)
        assertNull(parsed?.stationName)
    }
}
