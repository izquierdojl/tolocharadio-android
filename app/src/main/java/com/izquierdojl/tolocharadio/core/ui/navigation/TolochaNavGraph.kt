package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.LocalTolochaColors
import com.izquierdojl.tolocharadio.core.util.CastPermissions
import com.izquierdojl.tolocharadio.domain.podcasts.QueueBadge
import com.izquierdojl.tolocharadio.domain.servers.StartupGate
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutLaunchResolution
import com.izquierdojl.tolocharadio.feature.customstations.CustomStationsScreen
import com.izquierdojl.tolocharadio.feature.explore.ExploreScreen
import com.izquierdojl.tolocharadio.feature.explore.StationDetailScreen
import com.izquierdojl.tolocharadio.feature.favorites.FavoritesScreen
import com.izquierdojl.tolocharadio.feature.history.HistoryScreen
import com.izquierdojl.tolocharadio.feature.home.HomeScreen
import com.izquierdojl.tolocharadio.feature.player.MiniPlayer
import com.izquierdojl.tolocharadio.feature.player.PlayerViewModel
import com.izquierdojl.tolocharadio.feature.player.SleepTimerViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.ModeViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.discover.DiscoverScreen
import com.izquierdojl.tolocharadio.feature.podcasts.favorites.PodcastFavoritesScreen
import com.izquierdojl.tolocharadio.feature.podcasts.home.PodcastHomeScreen
import com.izquierdojl.tolocharadio.feature.podcasts.player.EpisodeFullPlayerSheet
import com.izquierdojl.tolocharadio.feature.podcasts.player.EpisodeMiniPlayer
import com.izquierdojl.tolocharadio.feature.podcasts.queue.QueueBadgeViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.queue.QueueScreen
import com.izquierdojl.tolocharadio.feature.podcasts.show.PodcastShowScreen
import com.izquierdojl.tolocharadio.feature.podcasts.stats.PodcastStatsScreen
import com.izquierdojl.tolocharadio.feature.podcasts.subscriptions.SubscriptionsScreen
import com.izquierdojl.tolocharadio.feature.servers.ServerFormScreen
import com.izquierdojl.tolocharadio.feature.servers.ServerListScreen
import com.izquierdojl.tolocharadio.feature.settings.SettingsScreen
import com.izquierdojl.tolocharadio.feature.shortcuts.ShortcutLaunchViewModel
import com.izquierdojl.tolocharadio.feature.stats.StatsScreen
import com.tolocharadio.domain.notification.NotificationAction
import com.tolocharadio.domain.notification.NotificationLogger
import com.tolocharadio.ui.notification.NotificationState
import com.tolocharadio.ui.notification.NotificationViewModel
import kotlinx.coroutines.launch

/** Duración del fade de entrada/salida entre destinos (spec 0061, FR-013). */
private const val NAV_ENTER_DURATION_MS = 200
private const val NAV_EXIT_DURATION_MS = 180

/**
 * Shell con bottom bar estilo Pocket Casts + mini-player persistente.
 *
 * Autenticación por servidor sin pantallas de login (FR-010/FR-011):
 * - sin servidores → formulario unificado (alta) bloqueante;
 * - servidor de arranque sin credenciales → formulario (edición) bloqueante;
 * - con credenciales → sesión automática y pantalla de arranque.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod")
@Composable
fun TolochaNavGraph(
    modifier: Modifier = Modifier,
    startupGate: StartupGate = StartupGate.NoServers,
    startupServerId: String? = null,
    startScreen: StartScreen = StartScreen.EXPLORE,
    podcastStartScreen: PodcastStartScreen = PodcastStartScreen.HOME,
    defaultSection: ContentMode = ContentMode.RADIO,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val chromeVisible = currentRoute != null && currentRoute != Routes.SERVER_FORM
    val context = LocalContext.current
    // Un único PlayerViewModel a ámbito de Activity (spec 004, R3).
    val playerVm: PlayerViewModel = hiltViewModel(context as ComponentActivity)
    // SleepTimerViewModel a ámbito de Activity (spec 014).
    val sleepTimerVm: SleepTimerViewModel = hiltViewModel(context)
    // Modo Radio|Podcasts + reproductor de episodios a ámbito de Activity (spec 0046).
    val modeVm: ModeViewModel = hiltViewModel(context)
    val modeUi by modeVm.ui.collectAsState()
    val episodeVm: EpisodePlayerViewModel = hiltViewModel(context)
    val episodeUi by episodeVm.ui.collectAsState()
    val queueBadgeVm: QueueBadgeViewModel = hiltViewModel(context)
    val queueBadge by queueBadgeVm.badge.collectAsState()
    val podcastMode = modeUi.podcastsEnabled && modeUi.mode == ContentMode.PODCAST
    var episodeFullPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        sleepTimerVm.setStopPlayerCallback {
            // El episodio reporta su posición antes de que la radio pare el player compartido.
            episodeVm.stop()
            playerVm.stop()
        }
    }
    LaunchedEffect(podcastMode) {
        if (podcastMode) queueBadgeVm.refresh()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (podcastMode) queueBadgeVm.refresh()
    }
    LaunchedEffect(episodeUi.episode?.id) {
        queueBadgeVm.setPlayingId(episodeUi.episode?.id)
    }
    // Keepalive de progreso al pasar a segundo plano (research D4).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        episodeVm.onAppBackground()
    }
    val snackbar = remember { SnackbarHostState() }

    ShortcutRouteEffects(
        playerVm = playerVm,
        snackbar = snackbar,
    )
    NotificationRouteEffects(
        navController = navController,
        playerVm = playerVm,
        snackbar = snackbar,
    )

    CastPermissionEffects(chromeVisible = chromeVisible)

    // FR-005: con servidor, abrir directamente en la pantalla de arranque
    // (spec 0046: sección inicial Radio|Podcasts).
    PodcastRouteEffects(
        navController = navController,
        currentRoute = currentRoute,
        podcastsEnabled = modeUi.podcastsEnabled,
        podcastMode = podcastMode,
        modeLoading = modeUi.loading,
        startScreen = startScreen,
        podcastStartScreen = podcastStartScreen,
        defaultSection = defaultSection,
        startupGate = startupGate,
    )

    val editActiveServer = {
        navController.navigate(Routes.serverForm(startupServerId)) {
            launchSingleTop = true
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sleepTimerUiState by sleepTimerVm.uiState.collectAsState()
    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }
    val closeMenu: () -> Unit = { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = chromeVisible,
        drawerContent = {
            TolochaDrawerContent(
                sleepTimerUiState = sleepTimerUiState,
                onStartTimer = sleepTimerVm::start,
                onCancelTimer = sleepTimerVm::cancel,
                onServers = {
                    closeMenu()
                    navController.navigate(Routes.SERVERS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSettings = {
                    closeMenu()
                    navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                },
            )
        },
    ) {
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (chromeVisible) {
                    TolochaTopBar(
                        mode = modeUi.mode,
                        podcastsEnabled = modeUi.podcastsEnabled,
                        onSelectMode = modeVm::setMode,
                        onOpenMenu = openMenu,
                        castAction = {
                            AndroidView(
                                factory = { ctx ->
                                    MediaRouteButton(ctx).apply {
                                        CastButtonFactory.setUpMediaRouteButton(ctx, this)
                                    }
                                },
                            )
                        },
                    )
                }
            },
            bottomBar = {
                if (chromeVisible) {
                    Column {
                        TolochaMiniPlayer(
                            episodeUi = episodeUi,
                            playerVm = playerVm,
                            snackbar = snackbar,
                            onEditServer = editActiveServer,
                            onOpenEpisode = { episodeFullPlayer = true },
                        )
                        HorizontalDivider(color = LocalTolochaColors.current.chromeDivider)
                        TolochaNavigationBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            dests = if (podcastMode) PODCAST_DESTS else BOTTOM_DESTS,
                            queueBadge = queueBadge,
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
                enterTransition = { fadeIn(tween(NAV_ENTER_DURATION_MS)) },
                exitTransition = { fadeOut(tween(NAV_EXIT_DURATION_MS)) },
                popEnterTransition = { fadeIn(tween(NAV_ENTER_DURATION_MS)) },
                popExitTransition = { fadeOut(tween(NAV_EXIT_DURATION_MS)) },
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
                    ExploreScreen(
                        onStation = { navController.navigate(Routes.stationDetail(it)) },
                        player = playerVm,
                    )
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
                    SettingsScreen(onStats = { navController.navigate(Routes.statsRoute(podcastMode)) })
                }
                composable(Routes.STATS) {
                    StatsScreen(onEditServer = editActiveServer)
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
                composable(Routes.PODCAST_HOME) {
                    PodcastHomeScreen(
                        onDiscover = { navController.navigate(Routes.PODCAST_DISCOVER) },
                        onQueue = { navController.navigate(Routes.PODCAST_QUEUE) },
                        onSubscriptions = { navController.navigate(Routes.PODCAST_SUBSCRIPTIONS) },
                        onOpenShow = { navController.navigate(Routes.podcastShow(it)) },
                        snackbar = snackbar,
                    )
                }
                composable(Routes.PODCAST_DISCOVER) {
                    DiscoverScreen(
                        onBrowseSubscriptions = { navController.navigate(Routes.PODCAST_SUBSCRIPTIONS) },
                        onEditServer = editActiveServer,
                        snackbar = snackbar,
                    )
                }
                composable(Routes.PODCAST_SUBSCRIPTIONS) {
                    SubscriptionsScreen(
                        onDiscover = { navController.navigate(Routes.PODCAST_DISCOVER) },
                        onOpenShow = { navController.navigate(Routes.podcastShow(it)) },
                        snackbar = snackbar,
                    )
                }
                composable(Routes.PODCAST_QUEUE) {
                    QueueScreen(
                        snackbar = snackbar,
                        onOpenFullPlayer = { episodeFullPlayer = true },
                    )
                }
                composable(Routes.PODCAST_FAVORITES) {
                    PodcastFavoritesScreen(snackbar = snackbar)
                }
                composable(Routes.PODCAST_STATS) {
                    PodcastStatsScreen(onEditServer = editActiveServer)
                }
                composable(
                    route = Routes.PODCAST_SHOW,
                    arguments = listOf(navArgument("podcastId") { type = NavType.LongType }),
                ) { entry ->
                    PodcastShowScreen(
                        podcastId = entry.arguments?.getLong("podcastId") ?: 0L,
                        onBack = { navController.popBackStack() },
                        snackbar = snackbar,
                    )
                }
            }
        }
    }
    if (episodeFullPlayer && episodeUi.episode != null) {
        EpisodeFullPlayerSheet(onDismiss = { episodeFullPlayer = false })
    }
}

/** Mini-player persistente: episodio en curso o radio (spec 0046). */
@Composable
private fun TolochaMiniPlayer(
    episodeUi: com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi,
    playerVm: PlayerViewModel,
    snackbar: SnackbarHostState,
    onEditServer: () -> Unit,
    onOpenEpisode: () -> Unit,
) {
    if (episodeUi.episode != null) {
        EpisodeMiniPlayer(onOpenFullPlayer = onOpenEpisode)
    } else {
        MiniPlayer(viewModel = playerVm, snackbar = snackbar, onEditServer = onEditServer)
    }
}

/**
 * Permisos de descubrimiento Cast (mDNS/red local): los pide al mostrar
 * el chrome y los reevalúa al volver de Ajustes.
 */
@Composable
private fun CastPermissionEffects(chromeVisible: Boolean) {
    val context = LocalContext.current
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
}

/**
 * Efectos de atajos del icono (spec 0018) y tap de notificación
 * (spec 0016): extraídos del shell para contener su complejidad.
 */
@Composable
private fun ShortcutRouteEffects(
    playerVm: PlayerViewModel,
    snackbar: SnackbarHostState,
) {
    val context = LocalContext.current
    val shortcutVm: ShortcutLaunchViewModel = hiltViewModel(context as ComponentActivity)
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
}

@Composable
private fun NotificationRouteEffects(
    navController: androidx.navigation.NavHostController,
    playerVm: PlayerViewModel,
    snackbar: SnackbarHostState,
) {
    val context = LocalContext.current
    val notificationVm: NotificationViewModel = hiltViewModel(context as ComponentActivity)
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
}

/**
 * Puerta del arranque: espera a que la sección y las capacidades estén
 * resueltas, a estar en la ruta inicial y a no haberlo aplicado antes.
 * Resolver antes daría un fallback a Radio y consumiría la única oportunidad
 * de abrir la pantalla elegida.
 */
internal fun shouldApplyStartDestination(
    currentRoute: String?,
    startupGate: StartupGate,
    modeLoading: Boolean,
    alreadyApplied: Boolean,
): Boolean =
    !alreadyApplied &&
        !modeLoading &&
        startupGate == StartupGate.Ready &&
        currentRoute == Routes.HOME

/**
 * Arranque y puerta de modo podcast (spec 0046):
 * sección inicial Radio|Podcasts y redirección de rutas podcast sin
 * podcasts o fuera de modo a la pantalla Radio vigente, sin errores.
 */
@Composable
@Suppress("LongParameterList")
private fun PodcastRouteEffects(
    navController: androidx.navigation.NavHostController,
    currentRoute: String?,
    podcastsEnabled: Boolean,
    podcastMode: Boolean,
    modeLoading: Boolean,
    startScreen: StartScreen,
    podcastStartScreen: PodcastStartScreen,
    defaultSection: ContentMode,
    startupGate: StartupGate,
) {
    var startupApplied by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(
        currentRoute,
        startScreen,
        defaultSection,
        podcastStartScreen,
        startupGate,
        podcastsEnabled,
        modeLoading,
    ) {
        if (!shouldApplyStartDestination(currentRoute, startupGate, modeLoading, startupApplied)) {
            return@LaunchedEffect
        }
        val target =
            resolveStartDestination(
                startScreen = startScreen,
                defaultSection = defaultSection,
                podcastStartScreen = podcastStartScreen,
                podcastsEnabled = podcastsEnabled,
            )
        navController.navigate(target) {
            popUpTo(Routes.HOME) { inclusive = true }
            launchSingleTop = true
        }
        startupApplied = true
    }
    LaunchedEffect(currentRoute, podcastMode, podcastsEnabled, podcastStartScreen, modeLoading) {
        // No aplicar el guard de modo mientras las capacidades y la sección
        // inicial siguen cargando: rebotaría una ruta de podcast restaurada.
        if (modeLoading) return@LaunchedEffect
        val route = currentRoute ?: return@LaunchedEffect
        val target =
            Routes.routeAfterModeChange(
                currentRoute = route,
                podcastMode = podcastMode,
                podcastsEnabled = podcastsEnabled,
                podcastStartRoute = podcastStartScreen.route(),
            ) ?: return@LaunchedEffect
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

/**
 * Barra de navegación inferior (spec 012). Extraída para poder fijar con un test
 * de UI el ancho de los destinos del modo vigente (regresión del bug 0040).
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
    dests: List<BottomDest> = BOTTOM_DESTS,
    queueBadge: QueueBadge = QueueBadge(0),
) {
    val colors = LocalTolochaColors.current
    NavigationBar {
        // `NavigationBarItem` es una extensión de `RowScope`; al envolverlo en un
        // `Box` (BoxScope) hay que conservar el receptor del `Row` explícitamente.
        val rowScope = this
        dests.forEach { dest ->
            val badgeText = if (dest.route == Routes.PODCAST_QUEUE) queueBadge.displayText else null
            val selected =
                currentRoute == dest.route ||
                    (dest.route == Routes.EXPLORE && currentRoute == Routes.STATION_DETAIL) ||
                    (dest.route == Routes.PODCAST_SUBSCRIPTIONS && currentRoute == Routes.PODCAST_SHOW)
            val tooltipState = rememberTooltipState()
            // Tooltip de accesibilidad al mantener pulsado (0012, US1/AC3).
            Box(Modifier.weight(1f).testTag("bottom-dest-${dest.route}")) {
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
                            selected = selected,
                            onClick = { onNavigate(dest.route) },
                            modifier =
                                if (badgeText != null) {
                                    Modifier.semantics { stateDescription = queueBadge.contentDescription }
                                } else {
                                    Modifier
                                },
                            alwaysShowLabel = true,
                            colors =
                                NavigationBarItemDefaults.colors(
                                    indicatorColor = colors.navIndicator,
                                    selectedIconColor = colors.navSelectedContent,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = colors.navUnselectedContent,
                                    unselectedTextColor = colors.navUnselectedContent,
                                ),
                            label = {
                                Text(
                                    dest.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            icon = {
                                if (badgeText != null) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                modifier =
                                                    Modifier
                                                        .testTag(TolochaTestTags.QUEUE_BADGE)
                                                        .clearAndSetSemantics {
                                                            contentDescription = queueBadge.contentDescription
                                                        },
                                            ) {
                                                Text(badgeText)
                                            }
                                        },
                                    ) {
                                        Icon(dest.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(dest.icon, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
