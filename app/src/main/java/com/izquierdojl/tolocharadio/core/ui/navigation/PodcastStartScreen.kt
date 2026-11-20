package com.izquierdojl.tolocharadio.core.ui.navigation

/** Destinos de arranque disponibles en modo Podcasts (spec 0048, US5). */
enum class PodcastStartScreen {
    HOME,
    DISCOVER,
    SUBSCRIPTIONS,
    QUEUE,
    FAVORITES,
    ;

    /** Ruta de navegación asociada al destino. */
    fun route(): String =
        when (this) {
            HOME -> Routes.PODCAST_HOME
            DISCOVER -> Routes.PODCAST_DISCOVER
            SUBSCRIPTIONS -> Routes.PODCAST_SUBSCRIPTIONS
            QUEUE -> Routes.PODCAST_QUEUE
            FAVORITES -> Routes.PODCAST_FAVORITES
        }
}
