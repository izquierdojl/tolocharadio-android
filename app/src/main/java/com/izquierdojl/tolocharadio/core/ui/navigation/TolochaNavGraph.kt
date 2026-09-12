package com.izquierdojl.tolocharadio.core.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
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

    // Permisos de Cast (Android 12+ mDNS / Android 13+ nearby).
    var castPermissionsGranted by remember {
        mutableStateOf(areCastPermissionsGranted(context))
    }
    val castPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            castPermissionsGranted = results.values.all { it }
        }
    LaunchedEffect(chromeVisible) {
        if (chromeVisible && !castPermissionsGranted) {
            val perms = requiredCastPermissions()
            if (perms.isNotEmpty()) {
                castPermissionLauncher.launch(perms.toTypedArray())
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
                    NavigationBar {
                        BOTTOM_DESTS.forEach { dest ->
                            NavigationBarItem(
                                selected =
                                    currentRoute == dest.route ||
                                        (dest.route == Routes.EXPLORE && currentRoute == Routes.STATION_DETAIL),
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                            )
                        }
                    }
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
 * Returns the Cast-related permissions required for the current API level.
 * - API 33+ (Android 13): NEARBY_WIFI_DEVICES
 * - API 31-32 (Android 12-12L): ACCESS_FINE_LOCATION
 * - API <31: no runtime permission needed for mDNS discovery
 */
private fun requiredCastPermissions(): List<String> =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        else -> emptyList()
    }

/** Checks whether all Cast discovery permissions are already granted. */
private fun areCastPermissionsGranted(context: android.content.Context): Boolean =
    requiredCastPermissions().all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
