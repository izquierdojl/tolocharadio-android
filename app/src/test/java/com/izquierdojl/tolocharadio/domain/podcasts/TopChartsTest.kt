package com.izquierdojl.tolocharadio.domain.podcasts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Catálogo fijo y defaults del ranking (spec 0053, US1/US2). */
class TopChartsTest {
    @Test
    fun `hay 13 paises con ids y etiquetas unicos`() {
        assertEquals(13, TopCountry.entries.size)
        assertEquals(13, TopCountry.entries.map { it.id }.toSet().size)
        assertEquals(13, TopCountry.entries.map { it.label }.toSet().size)
        assertTrue(TopCountry.entries.all { it.id.matches(Regex("^[a-z]{2}$")) })
        assertTrue(TopCountry.entries.all { it.label.isNotBlank() })
    }

    @Test
    fun `el pais por defecto es Espana y los desconocidos caen a Espana`() {
        assertEquals(TopCountry.SPAIN, TopCountry.fromId(null))
        assertEquals(TopCountry.SPAIN, TopCountry.fromId(""))
        assertEquals(TopCountry.SPAIN, TopCountry.fromId("XX"))
        assertEquals(TopCountry.MEXICO, TopCountry.fromId("mx"))
    }

    @Test
    fun `hay 18 categorias mas todas con ids unicos`() {
        val genres = TopGenre.entries.filter { it != TopGenre.ALL }
        assertEquals(18, genres.size)
        assertEquals(18, genres.mapNotNull { it.id }.toSet().size)
        assertTrue(genres.all { it.id?.matches(Regex("^[0-9]{1,6}$")) == true })
        assertTrue(TopGenre.entries.all { it.label.isNotBlank() })
        assertNull(TopGenre.ALL.id)
    }

    @Test
    fun `el genero por defecto es todas y los desconocidos caen a todas`() {
        assertEquals(TopGenre.ALL, TopGenre.fromId(null))
        assertEquals(TopGenre.ALL, TopGenre.fromId(""))
        assertEquals(TopGenre.ALL, TopGenre.fromId("9999"))
        assertEquals(TopGenre.COMEDY, TopGenre.fromId("1303"))
    }

    @Test
    fun `TopChartsLoad arranca vacio y sin aviso`() {
        val load = TopChartsLoad()
        assertTrue(load.items.isEmpty())
        assertFalse(load.genreUnavailable)
    }
}
