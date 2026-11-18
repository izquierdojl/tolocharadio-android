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

    fun stationDetail(stationId: String) = "station/$stationId"

    /** Formulario unificado de servidor (alta si `serverId` es null). */
    fun serverForm(serverId: String?): String = "server-form?serverId=${serverId ?: ""}"
}
