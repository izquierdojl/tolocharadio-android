package com.example.tolocharadio.core.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tolocharadio.core.session.AuthState
import com.example.tolocharadio.core.session.SessionManager
import com.example.tolocharadio.core.ui.components.TolochaLogo
import com.example.tolocharadio.feature.auth.LoginScreen
import com.example.tolocharadio.feature.auth.RegisterScreen
import com.example.tolocharadio.feature.explore.ExploreScreen
import com.example.tolocharadio.feature.explore.StationDetailScreen
import com.example.tolocharadio.feature.favorites.FavoritesScreen
import com.example.tolocharadio.feature.home.HomeScreen
import com.example.tolocharadio.feature.onboarding.InstanceSetupScreen
import com.example.tolocharadio.feature.player.MiniPlayer
import com.example.tolocharadio.feature.profile.ProfileScreen

private data class BottomDest(val route: String, val label: String, val icon: ImageVector)

/** Paridad web: Explorar, Favoritos, Historial, Mis emisoras + Perfil (Lucide→Material, spec 002). */
private val BOTTOM_DESTS =
    listOf(
        BottomDest(Routes.EXPLORE, "Explorar", Icons.Filled.Search),
        BottomDest(Routes.FAVORITES, "Favoritos", Icons.Filled.Favorite),
        BottomDest(Routes.HISTORY, "Historial", Icons.Filled.History),
        BottomDest(Routes.CUSTOM_STATIONS, "Mis emisoras", Icons.Filled.Radio),
        BottomDest(Routes.PROFILE, "Perfil", Icons.Filled.Person),
    )

/**
 * Shell con bottom bar estilo Pocket Casts + mini-player persistente.
 *
 * @param hasInstance false en primer arranque (va a [Routes.SETUP]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TolochaNavGraph(
    sessionManager: SessionManager,
    hasInstance: Boolean,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val authState by sessionManager.authState.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val chromeVisible = currentRoute != null && currentRoute !in setOf(Routes.SETUP, Routes.LOGIN, Routes.REGISTER)

    Scaffold(
        modifier = modifier,
        topBar = {
            if (chromeVisible) {
                TopAppBar(
                    title = { TolochaLogo() },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
                        ),
                )
            }
        },
        bottomBar = {
            if (chromeVisible) {
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
                            label = { Text(dest.label) },
                        )
                    }
                }
                MiniPlayer()
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
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.FAVORITES) {
                if (authState is AuthState.Authenticated) {
                    FavoritesScreen(
                        onStation = { navController.navigate(Routes.stationDetail(it)) },
                        onExplore = { navController.navigate(Routes.EXPLORE) },
                    )
                } else {
                    LoginScreen(onLoggedIn = {}, onRegister = { navController.navigate(Routes.REGISTER) })
                }
            }
            composable(Routes.HISTORY) {
                HomeScreen(onExplore = { navController.navigate(Routes.EXPLORE) })
            }
            composable(Routes.CUSTOM_STATIONS) {
                HomeScreen(onExplore = { navController.navigate(Routes.EXPLORE) })
            }
            composable(Routes.PROFILE) {
                if (authState is AuthState.Authenticated) {
                    ProfileScreen()
                } else {
                    LoginScreen(onLoggedIn = {}, onRegister = { navController.navigate(Routes.REGISTER) })
                }
            }
        }
    }
}
