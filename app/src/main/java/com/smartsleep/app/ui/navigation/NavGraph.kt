package com.smartsleep.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartsleep.app.R
import com.smartsleep.app.SmartSleepApplication
import com.smartsleep.app.data.repository.SleepRepository
import com.smartsleep.app.ui.common.GenericViewModelFactory
import com.smartsleep.app.ui.history.HistoryScreen
import com.smartsleep.app.ui.history.HistoryViewModel
import com.smartsleep.app.ui.home.HomeScreen
import com.smartsleep.app.ui.home.HomeViewModel
import com.smartsleep.app.ui.settings.SettingsScreen
import com.smartsleep.app.ui.settings.SettingsViewModel

private sealed class Dest(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Dest("home", R.string.nav_home, Icons.Filled.Home)
    data object History : Dest("history", R.string.nav_history, Icons.Filled.History)
    data object Settings : Dest("settings", R.string.nav_settings, Icons.Filled.Settings)
}

@Composable
fun SmartSleepNavGraph(repository: SleepRepository) {
    val navController = rememberNavController()
    val items = listOf(Dest.Home, Dest.History, Dest.Settings)

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = GenericViewModelFactory(repository) { SettingsViewModel(it) }
    )
    val settings by settingsViewModel.settings.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                items.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = GenericViewModelFactory(repository) { HomeViewModel(it) }
                )
                HomeScreen(homeViewModel, settings)
            }
            composable(Dest.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = GenericViewModelFactory(repository) { HistoryViewModel(it) }
                )
                HistoryScreen(historyViewModel, settings.use24HourFormat)
            }
            composable(Dest.Settings.route) {
                SettingsScreen(settingsViewModel)
            }
        }
    }
}
