package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionSearchScope
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionsVisibilityTest {
    private fun podcast(
        id: Long,
        title: String,
    ) = PodcastSummary(id = id, feedUrl = "https://feed.example/$id", title = title)

    private val items =
        listOf(
            podcast(1, "El Camión Fantasma"),
            podcast(2, "Otra historia"),
        )

    @Test
    fun `en podcasts filtra por titulo sin tildes`() {
        assertEquals(
            listOf("El Camión Fantasma"),
            visibleSubscriptions(items, "camion", SubscriptionSearchScope.PODCASTS)
                .map { it.title },
        )
    }

    @Test
    fun `en episodios la lista permanece completa`() {
        assertEquals(
            items,
            visibleSubscriptions(items, "camion", SubscriptionSearchScope.EPISODES),
        )
    }

    @Test
    fun `sin termino devuelve la lista completa`() {
        assertEquals(
            items,
            visibleSubscriptions(items, "  ", SubscriptionSearchScope.PODCASTS),
        )
    }
}
