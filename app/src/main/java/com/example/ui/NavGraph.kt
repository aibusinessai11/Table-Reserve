package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.viewmodel.RestaurantViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail")
    object Route : Screen("route")
    object Reservations : Screen("reservations")
    object Loyalty : Screen("loyalty")
}

@Composable
fun AppNavigation(
    viewModel: RestaurantViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // SnackBar state for displaying loyalty rewards/booking notifications
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Listen to ViewModel system alerts
    LaunchedEffect(key1 = true) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Determine if we should show bottom navigation bar (only on main screens)
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Reservations.route,
        Screen.Loyalty.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_bar")
                ) {
                    val items = listOf(
                        NavigationItem(
                            title = "Поиск",
                            route = Screen.Home.route,
                            selectedIcon = Icons.Filled.Restaurant,
                            unselectedIcon = Icons.Outlined.Restaurant
                        ),
                        NavigationItem(
                            title = "Бронирования",
                            route = Screen.Reservations.route,
                            selectedIcon = Icons.Filled.EventNote,
                            unselectedIcon = Icons.Outlined.EventNote
                        ),
                        NavigationItem(
                            title = "Клуб лояльности",
                            route = Screen.Loyalty.route,
                            selectedIcon = Icons.Filled.CardGiftcard,
                            unselectedIcon = Icons.Outlined.CardGiftcard
                        )
                    )

                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag("bottom_nav_${item.route}")
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = {
                        navController.navigate(Screen.Detail.route)
                    },
                    onNavigateToRoute = {
                        navController.navigate(Screen.Route.route)
                    }
                )
            }

            composable(Screen.Detail.route) {
                RestaurantDetailScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSuccessBooking = {
                        // Navigate to reservations tab
                        navController.navigate(Screen.Reservations.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                )
            }

            composable(Screen.Route.route) {
                RouteScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Reservations.route) {
                ReservationsScreen(
                    viewModel = viewModel,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                        }
                    }
                )
            }

            composable(Screen.Loyalty.route) {
                LoyaltyScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val route: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
