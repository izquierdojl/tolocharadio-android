package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector

/** Destino de la barra inferior (spec 012, TolochaNavigationBar). */
internal data class BottomDest(val route: String, val label: String, val icon: ImageVector)

/** Explorar, Favoritos, Historial y Mis emisoras (contenido; spec 0047). */
internal val BOTTOM_DESTS =
    listOf(
        BottomDest(Routes.EXPLORE, "Explorar", Icons.Filled.Search),
        BottomDest(Routes.FAVORITES, "Favoritos", Icons.Filled.Favorite),
        BottomDest(Routes.HISTORY, "Historial", Icons.Filled.History),
        BottomDest(Routes.CUSTOM_STATIONS, "Mis emisoras", Icons.Filled.Radio),
    )

/** Barra de podcast (clarify 2026-11-18): Inicio, Descubrir, Suscripciones, Cola, Favoritos. */
internal val PODCAST_DESTS =
    listOf(
        BottomDest(Routes.PODCAST_HOME, "Inicio", Icons.Filled.Home),
        BottomDest(Routes.PODCAST_DISCOVER, "Descubrir", Icons.Filled.Search),
        BottomDest(Routes.PODCAST_SUBSCRIPTIONS, "Suscripciones", Icons.Filled.Subscriptions),
        BottomDest(Routes.PODCAST_QUEUE, "Cola", Icons.Filled.QueueMusic),
        BottomDest(Routes.PODCAST_FAVORITES, "Favoritos", Icons.Filled.Favorite),
    )

internal val PODCAST_ROUTES =
    setOf(
        Routes.PODCAST_HOME,
        Routes.PODCAST_DISCOVER,
        Routes.PODCAST_SUBSCRIPTIONS,
        Routes.PODCAST_QUEUE,
        Routes.PODCAST_FAVORITES,
        Routes.PODCAST_STATS,
        Routes.PODCAST_SHOW,
    )

internal val RADIO_CONTENT_ROUTES =
    setOf(
        Routes.EXPLORE,
        Routes.STATION_DETAIL,
        Routes.FAVORITES,
        Routes.HISTORY,
        Routes.CUSTOM_STATIONS,
    )
