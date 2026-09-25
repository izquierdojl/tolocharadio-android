package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.mediarouter.app.MediaRouteButton
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.cast.framework.CastButtonFactory
import com.izquierdojl.tolocharadio.core.ui.components.TolochaLogo
import com.izquierdojl.tolocharadio.core.ui.components.ViewModeToggle
import com.izquierdojl.tolocharadio.core.util.CastPermissions
import com.izquierdojl.tolocharadio.domain.servers.StartupGate
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutLaunchResolution
import com.izquierdojl.tolocharadio.feature.ViewModeViewModel
import com.izquierdojl.tolocharadio.feature.customstations.CustomStationsScreen
import com.izquierdojl.tolocharadio.feature.explore.ExploreScreen
import com.izquierdojl.tolocharadio.feature.explore.StationDetailScreen
import com.izquierdojl.tolocharadio.feature.favorites.FavoritesScreen
import com.izquierdojl.tolocharadio.feature.history.HistoryScreen
import com.izquierdojl.tolocharadio.feature.home.HomeScreen
import com.izquierdojl.tolocharadio.feature.player.MiniPlayer
import com.izquierdojl.tolocharadio.feature.player.PlayerViewModel
import com.izquierdojl.tolocharadio.feature.player.SleepTimerButton
import com.izquierdojl.tolocharadio.feature.player.SleepTimerViewModel
import com.izquierdojl.tolocharadio.feature.servers.ServerFormScreen
import com.izquierdojl.tolocharadio.feature.servers.ServerListScreen
import com.izquierdojl.tolocharadio.feature.settings.SettingsScreen
import com.izquierdojl.tolocharadio.feature.shortcuts.ShortcutLaunchViewModel
import com.tolocharadio.domain.notification.NotificationAction
import com.tolocharadio.domain.notification.NotificationLogger
import com.tolocharadio.ui.notification.NotificationState
import com.tolocharadio.ui.notification.NotificationViewModel

private data class BottomDest(val route: String, val label: String, val icon: ImageVector)

/** Secciones con listas de emisoras: el alternador de vista es visible (spec 008, FR-001/FR-009). */
private val VIEW_MODE_ROUTES = setOf(Routes.EXPLORE, Routes.FAVORITES, Routes.HISTORY, Routes.CUSTOM_STATIONS)

/** Explorar, Favoritos, Historial, Mis emisoras + Configuración (FR-005). */
private val BOTTOM_DESTS =
    listOf(
        BottomDest(Routes.EXPLORE, "Explorar", Icons.Filled.Search),
        BottomDest(Routes.FAVORITES, "Favoritos", Icons.Filled.Favorite),
        BottomDest(Routes.HISTORY, "Historial", Icons.Filled.History),
        BottomDest(Routes.CUSTOM_STATIONS, "Mis emisoras", Icons.Filled.Radio),
        BottomDest(Routes.SETTINGS, "Configuración", Icons.Filled.Settings),
    )

/**
 * Shell con bottom bar estilo Pocket Casts + mini-player persistente.
 *
 * Autenticación por servidor sin pantallas de login (FR-010/FR-011):
 * - sin servidores → formulario unificado (alta) bloqueante;
 * - servidor de arranque sin credenciales → formulario (edición) bloqueante;
 * - con credenciales → sesión automática y pantalla de arranque.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TolochaNavGraph(
    modifier: Modifier = Modifier,
    startupGate: StartupGate = StartupGate.NoServers,
    startupServerId: String? = null,
    startScreen: StartScreen = StartScreen.EXPLORE,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val chromeVisible = currentRoute != null && currentRoute != Routes.SERVER_FORM
    val context = LocalContext.current
    // Un único PlayerViewModel a ámbito de Activity (spec 004, R3).
    val playerVm: PlayerViewModel = hiltViewModel(context as ComponentActivity)
    // Un único ViewModeViewModel a ámbito de Activity (spec 008).
    val viewModeVm: ViewModeViewModel = hiltViewModel(context)
    val viewMode by viewModeVm.mode.collectAsState()
    // SleepTimerViewModel a ámbito de Activity (spec 014).
    val sleepTimerVm: SleepTimerViewModel = hiltViewModel(context)
    val sleepTimerUiState by sleepTimerVm.uiState.collectAsState()
    LaunchedEffect(Unit) {
        sleepTimerVm.setStopPlayerCallback { playerVm.stop() }
    }
    val snackbar = remember { SnackbarHostState() }

    // Accesos directos del icono (spec 0018).
    val shortcutVm: ShortcutLaunchViewModel = hiltViewModel(context)
    val pendingShortcut by shortcutVm.pending.collectAsState()
    LaunchedEffect(pendingShortcut) {
        val request = pendingShortcut ?: return@LaunchedEffect
        when (val resolution = shortcutVm.resolve(request.stationId)) {
            is ShortcutLaunchResolution.Play -> {
                playerVm.play(resolution.station)
                playerVm.openFullPlayer()
                shortcutVm.consume()
            }
            is ShortcutLaunchResolution.Unavailable -> {
                shortcutVm.consume()
                snackbar.showSnackbar(resolution.message)
            }
        }
    }

    // Tap de notificación: enfoca reproductor, contenido o home (spec 0016, FR-003/FR-004).
    val notificationVm: NotificationViewModel = hiltViewModel(context)
    val notificationNav by notificationVm.notificationState.collectAsState()
    LaunchedEffect(notificationNav) {
        when (val s = notificationNav) {
            is NotificationState.Navigate -> {
                when (s.action) {
                    NotificationAction.OPEN_PLAYER -> playerVm.openFullPlayer()
                    NotificationAction.OPEN_CONTENT,
                    NotificationAction.OPEN_INFO,
                    ->
                        if (s.contentId != null) {
                            navController.navigate(Routes.stationDetail(s.contentId))
                        } else {
                            navController.navigate(Routes.HOME) { launchSingleTop = true }
                        }
                    NotificationAction.OPEN_MAIN ->
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                }
                notificationVm.resetState()
            }
            is NotificationState.Error -> {
                NotificationLogger.logError("Error de navegación por notificación: ${s.message}")
                snackbar.showSnackbar("No se pudo abrir la notificación")
                notificationVm.resetState()
            }
            else -> Unit
        }
    }

    // Cast: descubrir dispositivos es una operación de red local (mDNS).
    // Android 12 usa ACCESS_FINE_LOCATION, Android 13+ NEARBY_WIFI_DEVICES y
    // Android 17 (API 37) exige ACCESS_LOCAL_NETWORK o el selector queda vacío.
    var castPermissionsGranted by remember {
        mutableStateOf(CastPermissions.areGranted(context))
    }
    val castPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            // Reevaluar contra el sistema: en API 37 el grupo NEARBY_DEVICES
            // puede conceder ACCESS_LOCAL_NETWORK de forma implícita.
            castPermissionsGranted = CastPermissions.areGranted(context)
        }
    // Al volver de Ajustes refrescar el estado por si el usuario concedió a mano.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        castPermissionsGranted = CastPermissions.areGranted(context)
    }
    LaunchedEffect(chromeVisible, castPermissionsGranted) {
        if (chromeVisible && !castPermissionsGranted) {
            val permissions = CastPermissions.required()
            if (permissions.isNotEmpty()) {
                castPermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    // FR-005: con servidor, abrir directamente en la pantalla de arranque.
    LaunchedEffect(startScreen, startupGate) {
        if (currentRoute != Routes.HOME) return@LaunchedEffect
        val target =
            when (startScreen) {
                StartScreen.FAVORITES -> Routes.FAVORITES
                StartScreen.HISTORY -> Routes.HISTORY
                StartScreen.EXPLORE -> Routes.EXPLORE
            }
        navController.navigate(target) {
            popUpTo(Routes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    val editActiveServer = {
        navController.navigate(Routes.serverForm(startupServerId)) {
            launchSingleTop = true
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (chromeVisible) {
                TopAppBar(
                    title = { TolochaLogo() },
                    actions = {
                        // Botón Cast (Chromecast) — FR-001, FR-002, FR-003
                        AndroidView(
                            factory = { ctx ->
                                MediaRouteButton(ctx).apply {
                                    CastButtonFactory.setUpMediaRouteButton(ctx, this)
                                }
                            },
                            modifier = Modifier,
                        )
                        if (currentRoute in VIEW_MODE_ROUTES) {
                            ViewModeToggle(mode = viewMode, onToggle = viewModeVm::toggle)
                        }
                        SleepTimerButton(
                            uiState = sleepTimerUiState,
                            onStart = sleepTimerVm::start,
                            onCancel = sleepTimerVm::cancel,
                        )
                        IconButton(
                            onClick = {
                                navController.navigate(Routes.SERVERS) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        ) {
                            Icon(Icons.Filled.Dns, contentDescription = "Servidores")
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
                        ),
                )
            }
        },
        bottomBar = {
            if (chromeVisible) {
                Column {
                    MiniPlayer(viewModel = playerVm, snackbar = snackbar)
                    TolochaNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        val startDestination =
            when (startupGate) {
                StartupGate.NoServers -> Routes.serverForm(null)
                StartupGate.NeedsCredentials -> Routes.serverForm(startupServerId)
                StartupGate.Ready -> Routes.HOME
            }
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(
                route = Routes.SERVER_FORM,
                arguments =
                    listOf(
                        navArgument("serverId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
            ) { entry ->
                val serverId = entry.arguments?.getString("serverId")?.takeIf { it.isNotBlank() }
                ServerFormScreen(
                    serverId = serverId,
                    showCancel = startupGate == StartupGate.Ready,
                    onDone = {
                        if (startupGate == StartupGate.Ready) {
                            navController.popBackStack()
                        } else {
                            // Recrea el grafo Hilt/Retrofit contra el servidor nuevo.
                            com.jakewharton.processphoenix.ProcessPhoenix.triggerRebirth(context)
                        }
                    },
                    onCancel = {
                        if (startupGate == StartupGate.Ready) navController.popBackStack()
                    },
                )
            }
            composable(Routes.HOME) { HomeScreen(onExplore = { navController.navigate(Routes.EXPLORE) }) }
            composable(Routes.EXPLORE) {
                ExploreScreen(onStation = { navController.navigate(Routes.stationDetail(it)) })
            }
            composable(Routes.STATION_DETAIL) {
                StationDetailScreen(
                    viewModel = hiltViewModel(),
                    player = playerVm,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onStation = { navController.navigate(Routes.stationDetail(it)) },
                    onExplore = { navController.navigate(Routes.EXPLORE) },
                    onEditServer = editActiveServer,
                    player = playerVm,
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onStation = { navController.navigate(Routes.stationDetail(it)) },
                    onExplore = { navController.navigate(Routes.EXPLORE) },
                    onEditServer = editActiveServer,
                    player = playerVm,
                )
            }
            composable(Routes.CUSTOM_STATIONS) {
                CustomStationsScreen(
                    onExplore = { navController.navigate(Routes.EXPLORE) },
                    player = playerVm,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(Routes.SERVERS) {
                ServerListScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.serverForm(null)) },
                    onEdit = { id -> navController.navigate(Routes.serverForm(id)) },
                    onNoServers = {
                        navController.navigate(Routes.serverForm(null)) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

/**
 * Barra de navegación inferior (spec 012). Extraída para poder fijar con un test
 * de UI el ancho de los 5 destinos (regresión del bug 0040).
 *
 * `TooltipBox` no es `RowScope` y su `content` es `@Composable () -> Unit`: si
 * envuelve directamente un [NavigationBarItem], el `Modifier.weight(1f)` del item
 * queda en un nodo que ya no es hijo directo del `Row` y la barra lo ignora, de
 * modo que el primer destino ocupa todo el ancho y el resto se mide a 0. El
 * `Box(Modifier.weight(1f))` restaura el `weight` en el hijo directo del `Row`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TolochaNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        // `NavigationBarItem` es una extensión de `RowScope`; al envolverlo en un
        // `Box` (BoxScope) hay que conservar el receptor del `Row` explícitamente.
        val rowScope = this
        BOTTOM_DESTS.forEach { dest ->
            val tooltipState = rememberTooltipState()
            // Tooltip de accesibilidad al mantener pulsado (0012, US1/AC3).
            Box(Modifier.weight(1f)) {
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above,
                        ),
                    tooltip = {
                        PlainTooltip { Text(dest.label) }
                    },
                    state = tooltipState,
                ) {
                    with(rowScope) {
                        NavigationBarItem(
                            selected =
                                currentRoute == dest.route ||
                                    (dest.route == Routes.EXPLORE && currentRoute == Routes.STATION_DETAIL),
                            onClick = { onNavigate(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                        )
                    }
                }
            }
        }
    }
}
