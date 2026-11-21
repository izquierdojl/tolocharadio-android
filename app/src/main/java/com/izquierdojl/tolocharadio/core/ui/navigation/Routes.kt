package com.izquierdojl.tolocharadio.core.ui.navigation

/** Destinos (`/, /explorar, /favoritos, /historial, /mis-emisoras, /configuracion, /servidores`). */
object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val STATION_DETAIL = "station/{stationId}"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val CUSTOM_STATIONS = "custom-stations"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val SERVERS = "servers"
    const val SERVER_FORM = "server-form?serverId={serverId}"

    // Podcast (spec 0046): portada, descubrir, suscripciones, cola, favoritos, ficha y stats.
    const val PODCAST_HOME = "podcasts"
    const val PODCAST_DISCOVER = "podcasts/discover"
    const val PODCAST_SUBSCRIPTIONS = "podcasts/subscriptions"
    const val PODCAST_QUEUE = "podcasts/queue"
    const val PODCAST_FAVORITES = "podcasts/favorites"
    const val PODCAST_STATS = "podcasts/stats"
    const val PODCAST_SHOW = "podcasts/{podcastId}"

    fun podcastShow(podcastId: Long) = "podcasts/$podcastId"

    /** Gráficas del modo activo (spec 0048, FR-049). */
    fun statsRoute(podcastMode: Boolean): String = if (podcastMode) PODCAST_STATS else STATS

    /**
     * Destino al cambiar de modo con una pantalla abierta. `null` si no
     * hay que navegar. Intercambia las dos rutas de Gráficas y mantiene
     * el resto del guard de modo (spec 0048, FR-052).
     */
    fun routeAfterModeChange(
        currentRoute: String?,
        podcastMode: Boolean,
        podcastsEnabled: Boolean,
        podcastStartRoute: String = PODCAST_HOME,
    ): String? =
        when {
            currentRoute == null -> null
            currentRoute == STATS && podcastMode -> PODCAST_STATS
            currentRoute == PODCAST_STATS && (!podcastsEnabled || !podcastMode) -> STATS
            currentRoute in PODCAST_ROUTES && (!podcastsEnabled || !podcastMode) -> EXPLORE
            podcastMode && currentRoute in RADIO_CONTENT_ROUTES -> podcastStartRoute
            else -> null
        }

    fun stationDetail(stationId: String) = "station/$stationId"

    /** Formulario unificado de servidor (alta si `serverId` es null). */
    fun serverForm(serverId: String?): String = "server-form?serverId=${serverId ?: ""}"
}
