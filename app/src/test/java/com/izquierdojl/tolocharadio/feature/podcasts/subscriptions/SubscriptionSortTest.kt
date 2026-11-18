package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionSortTest {
    private fun podcast(
        id: Long,
        title: String,
        latest: Long? = null,
    ) = PodcastSummary(
        id = id,
        feedUrl = "https://f.example/$id.xml",
        title = title,
        latestPendingEpisodeAt = latest,
    )

    private fun titles(items: List<PodcastSummary>) = items.map { it.title }

    @Test
    fun `nombre ascendente ignora mayusculas y acentos`() {
        val items =
            listOf(
                podcast(1, "álbum"),
                podcast(2, "Beta"),
                podcast(3, "Zeta"),
            )
        assertEquals(
            listOf("álbum", "Beta", "Zeta"),
            titles(sortSubscriptions(items, SubscriptionSort.NAME_ASC)),
        )
    }

    @Test
    fun `nombre descendente invierte el orden`() {
        val items =
            listOf(
                podcast(1, "álbum"),
                podcast(2, "Beta"),
                podcast(3, "Zeta"),
            )
        assertEquals(
            listOf("Zeta", "Beta", "álbum"),
            titles(sortSubscriptions(items, SubscriptionSort.NAME_DESC)),
        )
    }

    @Test
    fun `episodios recientes deja al final los programas sin pendientes`() {
        val items =
            listOf(
                podcast(1, "Sin pendientes"),
                podcast(2, "Antiguo", latest = 100),
                podcast(3, "Reciente", latest = 300),
            )
        assertEquals(
            listOf("Reciente", "Antiguo", "Sin pendientes"),
            titles(sortSubscriptions(items, SubscriptionSort.RECENT_EPISODES)),
        )
    }

    @Test
    fun `orden manual respeta los ids guardados y deja al final los nuevos`() {
        val items =
            listOf(
                podcast(1, "Uno"),
                podcast(2, "Dos"),
                podcast(3, "Tres"),
            )
        assertEquals(
            listOf("Tres", "Uno", "Dos"),
            titles(sortSubscriptions(items, SubscriptionSort.MANUAL, manualOrder = listOf(3, 1))),
        )
    }

    @Test
    fun `ultimas suscritas conserva el orden del servidor`() {
        val items =
            listOf(
                podcast(1, "Zeta"),
                podcast(2, "Alfa"),
            )
        assertEquals(
            listOf("Zeta", "Alfa"),
            titles(sortSubscriptions(items, SubscriptionSort.RECENT)),
        )
    }
}
