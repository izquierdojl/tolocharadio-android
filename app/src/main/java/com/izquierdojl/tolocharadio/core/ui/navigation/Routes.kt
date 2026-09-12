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
    const val SERVERS = "servers"
    const val SERVER_FORM = "server-form?serverId={serverId}"

    fun stationDetail(stationId: String) = "station/$stationId"

    /** Formulario unificado de servidor (alta si `serverId` es null). */
    fun serverForm(serverId: String?): String = "server-form?serverId=${serverId ?: ""}"
}
