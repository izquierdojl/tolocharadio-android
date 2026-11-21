package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionSearchTest {
    private fun podcast(
        id: Long,
        title: String,
    ) = PodcastSummary(id = id, feedUrl = "https://feed.example/$id", title = title)

    private fun titles(items: List<PodcastSummary>) = items.map { it.title }

    @Test
    fun `normaliza mayusculas y tildes`() {
        assertEquals("el camion fantasma", normalizeSearchText("El Camión Fantasma"))
    }

    @Test
    fun `filtro parcial ignora mayusculas y tildes`() {
        val items =
            listOf(
                podcast(1, "El Camión Fantasma"),
                podcast(2, "Otra historia"),
                podcast(3, "CAMINO al sur"),
            )

        assertEquals(
            listOf("El Camión Fantasma"),
            titles(filterSubscriptionsByTitle(items, "camion")),
        )
        assertEquals(
            listOf("El Camión Fantasma"),
            titles(filterSubscriptionsByTitle(items, "CAMIÓN")),
        )
        assertEquals(
            listOf("El Camión Fantasma", "CAMINO al sur"),
            titles(filterSubscriptionsByTitle(items, "cami")),
        )
    }

    @Test
    fun `filtro conserva el orden recibido`() {
        val items =
            listOf(
                podcast(1, "Zeta podcast"),
                podcast(2, "Alfa podcast"),
                podcast(3, "Beta podcast"),
            )

        assertEquals(
            listOf("Zeta podcast", "Alfa podcast"),
            titles(filterSubscriptionsByTitle(items, "podcast").take(2)),
        )
        assertEquals(
            listOf("Zeta podcast", "Alfa podcast", "Beta podcast"),
            titles(filterSubscriptionsByTitle(items, "podcast")),
        )
    }

    @Test
    fun `termino vacio o solo espacios devuelve todos`() {
        val items = listOf(podcast(1, "Uno"), podcast(2, "Dos"))

        assertEquals(items, filterSubscriptionsByTitle(items, ""))
        assertEquals(items, filterSubscriptionsByTitle(items, "   "))
    }

    @Test
    fun `termino con caracteres especiales no rompe la busqueda`() {
        val items = listOf(podcast(1, "C++ y Rust"), podcast(2, "Kotlin"))

        assertEquals(
            listOf("C++ y Rust"),
            titles(filterSubscriptionsByTitle(items, "c++")),
        )
    }
}
