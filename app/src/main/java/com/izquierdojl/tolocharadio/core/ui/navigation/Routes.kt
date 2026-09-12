package com.izquierdojl.tolocharadio.core.ui.navigation

/** Destinos (`/, /explorar, /favoritos, /historial, /mis-emisoras, /configuracion, /servidores`). */
object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val STATION_DETAIL = "station/{stationId}"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val CUSTOM_STATIONS = "custom-stations"
    const val SETTINGS = "settings"
    const val SERVERS = "servers"

    fun stationDetail(stationId: String) = "station/$stationId"
}
