package com.izquierdojl.tolocharadio.core.ui.navigation

/**
 * Resolución del destino de arranque a partir de las dos preferencias
 * (radio y podcast) y de la sección inicial (`data-model.md`
 * `StartDestination`).
 */
data class StartDestination(
    val radioStart: StartScreen,
    val podcastStart: PodcastStartScreen,
    val defaultSection: ContentMode,
    val podcastsEnabled: Boolean,
) {
    val route: String
        get() =
            if (defaultSection == ContentMode.PODCAST && podcastsEnabled) {
                podcastStart.route()
            } else {
                radioStart.route()
            }
}

/** Atajo puro para el arranque; devuelve la ruta final. */
fun resolveStartDestination(
    startScreen: StartScreen,
    defaultSection: ContentMode,
    podcastStartScreen: PodcastStartScreen,
    podcastsEnabled: Boolean,
): String =
    StartDestination(
        radioStart = startScreen,
        podcastStart = podcastStartScreen,
        defaultSection = defaultSection,
        podcastsEnabled = podcastsEnabled,
    ).route

private fun StartScreen.route(): String =
    when (this) {
        StartScreen.EXPLORE -> Routes.EXPLORE
        StartScreen.FAVORITES -> Routes.FAVORITES
        StartScreen.HISTORY -> Routes.HISTORY
    }
