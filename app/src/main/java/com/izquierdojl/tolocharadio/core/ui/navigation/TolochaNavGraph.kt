package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.activity.ComponentActivity
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.mediarouter.app.MediaRouteButton
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.cast.framework.CastButtonFactory
import com.izquierdojl.tolocharadio.core.session.AuthState
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.ui.components.TolochaLogo
import com.izquierdojl.tolocharadio.core.ui.components.ViewModeToggle
import com.izquierdojl.tolocharadio.feature.ViewModeViewModel
import com.izquierdojl.tolocharadio.feature.auth.LoginScreen
import com.izquierdojl.tolocharadio.feature.auth.RegisterScreen
import com.izquierdojl.tolocharadio.feature.customstations.CustomStationsScreen
import com.izquierdojl.tolocharadio.feature.explore.ExploreScreen
import com.izquierdojl.tolocharadio.feature.explore.StationDetailScreen
import com.izquierdojl.tolocharadio.feature.favorites.FavoritesScreen
import com.izquierdojl.tolocharadio.feature.history.HistoryScreen
import com.izquierdojl.tolocharadio.feature.home.HomeScreen
import com.izquierdojl.tolocharadio.feature.onboarding.InstanceSetupScreen
import com.izquierdojl.tolocharadio.feature.player.MiniPlayer
import com.izquierdojl.tolocharadio.feature.player.PlayerViewModel
import com.izquierdojl.tolocharadio.feature.player.SleepTimerButton
import com.izquierdojl.tolocharadio.feature.player.SleepTimerViewModel
import com.izquierdojl.tolocharadio.feature.servers.ServerListScreen
import com.izquierdojl.tolocharadio.feature.settings.SettingsScreen

private data class BottomDest(val route: String, val label: String, val icon: ImageVector)

/** Secciones con listas de emisoras: el alternador de vista es visible (spec 008, FR-001/FR-009). */
private val VIEW_MODE_ROUTES = setOf(Routes.EXPLORE, Routes.FAVORITES, Routes.HISTORY, Routes.CUSTOM_STATIONS)

/** Explorar, Favoritos, Historial, Mis emisoras + Configuración (FR-011). */
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
 * Servidores es una sección propia de primer nivel, accesible desde
 * la barra superior (FR-004).
 *
 * Sin sesión válida al arrancar, la app muestra la lista de
 * servidores guardados para seleccionar a cuál conectarse (FR-014);
 * si no hay servidores guardados, va a Login.
 *
 * @param hasInstance false en primer arranque (va a [Routes.SETUP]).
 * @param hasServers true si hay servidores guardados (FR-014).
 * @param startScreen pantalla de arranque con sesión restaurada (FR-011b).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TolochaNavGraph(
    sessionManager: SessionManager,
    hasInstance: Boolean,
    modifier: Modifier = Modifier,
    hasServers: Boolean = false,
    startScreen: StartScreen = StartScreen.EXPLORE,
) {
    val navController = rememberNavController()
    val authState by sessionManager.authState.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val chromeVisible = currentRoute != null && currentRoute !in setOf(Routes.SETUP, Routes.LOGIN, Routes.REGISTER)
    // Un único PlayerViewModel a ámbito de Activity (spec 004, R3): el panel
    // global y todas las pantallas comparten emisora y estado al navegar.
    val playerVm: PlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    // Un único ViewModeViewModel a ámbito de Activity (spec 008): la TopAppBar
    // compartida y las 4 secciones observan el mismo modo de vista (FR-004).
    val viewModeVm: ViewModeViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val viewMode by viewModeVm.mode.collectAsState()
    // SleepTimerViewModel a ámbito de Activity para persistir al navegar (spec 014).
    val sleepTimerVm: SleepTimerViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val sleepTimerUiState by sleepTimerVm.uiState.collectAsState()
    LaunchedEffect(Unit) {
        sleepTimerVm.setStopPlayerCallback { playerVm.stop() }
    }
    val snackbar = remember { SnackbarHostState() }

    // FR-011b: con sesión restaurada, abrir directamente en la pantalla
    // de arranque configurada. FR-014: sin sesión válida, mostrar la
    // lista de servidores para seleccionar a cuál conectarse (o Login
    // si no hay ninguno guardado). Solo actúa en HOME, es decir, en el
    // arranque de la app.
    LaunchedEffect(authState, startScreen, hasServers) {
        if (!hasInstance || currentRoute != Routes.HOME) return@LaunchedEffect
        when (authState) {
            is AuthState.Authenticated -> {
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
            is AuthState.Unauthenticated -> {
                val target = if (hasServers) Routes.SERVERS else Routes.LOGIN
                navController.navigate(target) {
                    popUpTo(Routes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
            }
            else -> Unit
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
                        // Alternador lista/tarjetas solo en secciones con
                        // listas de emisoras (spec 008, FR-001/FR-009).
                        if (currentRoute in VIEW_MODE_ROUTES) {
                            ViewModeToggle(mode = viewMode, onToggle = viewModeVm::toggle)
                        }
                        // Temporizador de apagado (spec 014, FR-001-FR-008).
                        SleepTimerButton(
                            uiState = sleepTimerUiState,
                            onStart = sleepTimerVm::start,
                            onCancel = sleepTimerVm::cancel,
                        )
                        // Servidores: sección propia de primer nivel (FR-004);
                        // sin sesión es la pantalla de selección de conexión (FR-014)
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
                // Panel justo encima de los botones (spec 004, FR-001).
                Column {
                    MiniPlayer(viewModel = playerVm, snackbar = snackbar)
                    NavigationBar {
                        BOTTOM_DESTS.forEach { dest ->
                            NavigationBarItem(
                                selected =
                                    currentRoute == dest.route ||
                                        (dest.route == Routes.EXPLORE && currentRoute == Routes.STATION_DETAIL),
                                onClick = {
                                    if (dest.route in AUTH_REQUIRED && authState !is AuthState.Authenticated) {
                                        navController.navigate(Routes.LOGIN)
                                    } else {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
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
        NavHost(
            navController = navController,
            startDestination = if (hasInstance) Routes.HOME else Routes.SETUP,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SETUP) {
                InstanceSetupScreen(onConnected = {
                    // La base cambió: renacer el proceso recrea el grafo
                    // Hilt (Retrofit) contra la instancia nueva.
                    com.jakewharton.processphoenix.ProcessPhoenix.triggerRebirth(navController.context)
                })
            }
            composable(Routes.HOME) { HomeScreen(onExplore = { navController.navigate(Routes.EXPLORE) }) }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        navController.navigate(Routes.EXPLORE) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(onRegistered = {
                    navController.navigate(Routes.EXPLORE) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                })
            }
            composable(Routes.EXPLORE) {
                if (authState is AuthState.Authenticated) {
                    ExploreScreen(onStation = { navController.navigate(Routes.stationDetail(it)) })
                } else {
                    LoginScreen(onLoggedIn = {}, onRegister = { navController.navigate(Routes.REGISTER) })
                }
            }
            composable(Routes.STATION_DETAIL) {
                StationDetailScreen(
                    viewModel = hiltViewModel(),
                    player = playerVm,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.FAVORITES) {
                if (authState is AuthState.Authenticated) {
                    FavoritesScreen(
                        onStation = { navController.navigate(Routes.stationDetail(it)) },
                        onExplore = { navController.navigate(Routes.EXPLORE) },
                        player = playerVm,
                    )
                } else {
                    LoginScreen(onLoggedIn = {}, onRegister = { navController.navigate(Routes.REGISTER) })
                }
            }
            composable(Routes.HISTORY) {
                if (authState is AuthState.Authenticated) {
                    HistoryScreen(
                        onStation = { navController.navigate(Routes.stationDetail(it)) },
                        onExplore = { navController.navigate(Routes.EXPLORE) },
                        player = playerVm,
                    )
                } else {
                    LoginScreen(onLoggedIn = {}, onRegister = { navController.navigate(Routes.REGISTER) })
                }
            }
            composable(Routes.CUSTOM_STATIONS) {
                CustomStationsDestination(
                    authState = authState,
                    onExplore = { navController.navigate(Routes.EXPLORE) },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    player = playerVm,
                )
            }
            composable(Routes.SETTINGS) {
                if (authState is AuthState.Authenticated) {
                    SettingsScreen(onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SETTINGS) { inclusive = true }
                        }
                    })
                } else {
                    LoginScreen(onLoggedIn = {}, onRegister = { navController.navigate(Routes.REGISTER) })
                }
            }
            composable(Routes.SERVERS) {
                // Accesible sin sesión: es el selector de conexión (FR-014).
                ServerListScreen(
                    onBack = { navController.popBackStack() },
                    onLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
        }
    }
}

/**
 * Destino Mis emisoras con guardia de sesión (patrón HISTORY/FAVORITES).
 * Extraído para no aumentar la complejidad ciclomática de [TolochaNavGraph].
 */
@Composable
private fun CustomStationsDestination(
    authState: AuthState,
    onExplore: () -> Unit,
    onRegister: () -> Unit,
    player: PlayerViewModel,
) {
    if (authState is AuthState.Authenticated) {
        CustomStationsScreen(onExplore = onExplore, player = player)
    } else {
        LoginScreen(onLoggedIn = {}, onRegister = onRegister)
    }
}
