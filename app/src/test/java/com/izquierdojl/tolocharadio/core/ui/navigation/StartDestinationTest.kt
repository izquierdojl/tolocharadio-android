package com.izquierdojl.tolocharadio.core.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {
    @Test
    fun `radio usa su pantalla de arranque`() {
        assertEquals(
            Routes.FAVORITES,
            resolveStartDestination(
                startScreen = StartScreen.FAVORITES,
                defaultSection = ContentMode.RADIO,
                podcastStartScreen = PodcastStartScreen.QUEUE,
                podcastsEnabled = true,
            ),
        )
    }

    @Test
    fun `podcast usa la pantalla elegida`() {
        assertEquals(
            Routes.PODCAST_QUEUE,
            resolveStartDestination(
                startScreen = StartScreen.FAVORITES,
                defaultSection = ContentMode.PODCAST,
                podcastStartScreen = PodcastStartScreen.QUEUE,
                podcastsEnabled = true,
            ),
        )
    }

    @Test
    fun `sin podcasts cae a la pantalla de radio`() {
        assertEquals(
            Routes.HISTORY,
            resolveStartDestination(
                startScreen = StartScreen.HISTORY,
                defaultSection = ContentMode.PODCAST,
                podcastStartScreen = PodcastStartScreen.QUEUE,
                podcastsEnabled = false,
            ),
        )
    }

    @Test
    fun `todos los destinos de podcast resuelven su ruta`() {
        val expected =
            mapOf(
                PodcastStartScreen.HOME to Routes.PODCAST_HOME,
                PodcastStartScreen.DISCOVER to Routes.PODCAST_DISCOVER,
                PodcastStartScreen.SUBSCRIPTIONS to Routes.PODCAST_SUBSCRIPTIONS,
                PodcastStartScreen.QUEUE to Routes.PODCAST_QUEUE,
                PodcastStartScreen.FAVORITES to Routes.PODCAST_FAVORITES,
            )
        expected.forEach { (screen, route) ->
            assertEquals(
                route,
                resolveStartDestination(
                    startScreen = StartScreen.EXPLORE,
                    defaultSection = ContentMode.PODCAST,
                    podcastStartScreen = screen,
                    podcastsEnabled = true,
                ),
            )
        }
    }

    @Test
    fun `statsRoute sigue el modo activo`() {
        assertEquals(Routes.STATS, Routes.statsRoute(podcastMode = false))
        assertEquals(Routes.PODCAST_STATS, Routes.statsRoute(podcastMode = true))
    }

    @Test
    fun `routeAfterModeChange intercambia las rutas de graficas`() {
        assertEquals(
            Routes.PODCAST_STATS,
            Routes.routeAfterModeChange(Routes.STATS, podcastMode = true, podcastsEnabled = true),
        )
        assertEquals(
            Routes.STATS,
            Routes.routeAfterModeChange(Routes.PODCAST_STATS, podcastMode = false, podcastsEnabled = true),
        )
        assertEquals(
            Routes.STATS,
            Routes.routeAfterModeChange(Routes.PODCAST_STATS, podcastMode = true, podcastsEnabled = false),
        )
    }

    @Test
    fun `routeAfterModeChange usa la pantalla de arranque de podcast`() {
        assertEquals(
            Routes.PODCAST_SUBSCRIPTIONS,
            Routes.routeAfterModeChange(
                Routes.EXPLORE,
                podcastMode = true,
                podcastsEnabled = true,
                podcastStartRoute = Routes.PODCAST_SUBSCRIPTIONS,
            ),
        )
        assertEquals(
            Routes.PODCAST_HOME,
            Routes.routeAfterModeChange(Routes.EXPLORE, podcastMode = true, podcastsEnabled = true),
        )
    }

    @Test
    fun `routeAfterModeChange mantiene el guard del resto de rutas`() {
        assertEquals(
            Routes.EXPLORE,
            Routes.routeAfterModeChange(Routes.PODCAST_HOME, podcastMode = false, podcastsEnabled = true),
        )
        assertEquals(
            Routes.PODCAST_HOME,
            Routes.routeAfterModeChange(Routes.EXPLORE, podcastMode = true, podcastsEnabled = true),
        )
        assertEquals(
            null,
            Routes.routeAfterModeChange(Routes.SETTINGS, podcastMode = true, podcastsEnabled = true),
        )
        assertEquals(null, Routes.routeAfterModeChange(null, podcastMode = true, podcastsEnabled = true))
    }
}
