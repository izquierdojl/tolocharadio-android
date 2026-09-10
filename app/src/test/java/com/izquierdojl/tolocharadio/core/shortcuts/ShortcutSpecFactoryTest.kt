package com.izquierdojl.tolocharadio.core.shortcuts

import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutSpecFactoryTest {
    private val factory = ShortcutSpecFactory()

    private fun station(
        id: String,
        name: String = "Radio",
        favicon: String? = null,
        rank: Int = 0,
    ) = ShortcutStation(stationId = id, name = name, faviconUrl = favicon, rank = rank)

    @Test
    fun `id determinista y estacion en el intent`() {
        val spec = factory.create(listOf(station("s1"))).single()
        assertEquals("hist-s1", spec.id)
        assertEquals(ShortcutIntents.ACTION_OPEN_STATION, spec.intentAction)
        assertEquals("s1", spec.intentStationId)
        assertEquals("Radio", spec.intentStationName)
    }

    @Test
    fun `conserva rank y url de icono`() {
        val spec = factory.create(listOf(station("s1", favicon = "https://x/f.png", rank = 3))).single()
        assertEquals(3, spec.rank)
        assertEquals("https://x/f.png", spec.iconUrl)
    }

    @Test
    fun `recorta etiquetas largas preservando el inicio`() {
        val longName = "Emisora ".repeat(12).trim()
        val spec = factory.create(listOf(station("s1", name = longName))).single()

        assertTrue(spec.shortLabel.length <= 25)
        assertTrue(spec.shortLabel.endsWith("…"))
        assertTrue(longName.startsWith(spec.shortLabel.dropLast(1)))

        assertTrue(spec.longLabel.length <= 60)
        assertTrue(spec.longLabel.endsWith("…"))
    }

    @Test
    fun `nombre vacio usa fallback`() {
        val spec = factory.create(listOf(station("s1", name = "  "))).single()
        assertEquals("Emisora", spec.intentStationName)
    }
}
