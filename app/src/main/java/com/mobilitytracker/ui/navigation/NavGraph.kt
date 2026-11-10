package com.mobilitytracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mobilitytracker.ui.calendar.CalendarScreen
import com.mobilitytracker.ui.settings.SettingsScreen
import com.mobilitytracker.ui.stats.StatsScreen
import com.mobilitytracker.ui.today.TodayScreen
import kotlinx.serialization.Serializable

@Serializable object Today
@Serializable object Calendar
@Serializable object Stats
@Serializable object Settings

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: Any
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val todayScrollState = rememberScrollState()
    val calendarListState = rememberLazyListState()
    val statsScrollState = rememberScrollState()
    val settingsScrollState = rememberScrollState()

    val navItems = listOf(
        NavItem("Dzisiaj", Icons.Outlined.Today, Icons.Filled.Today, Today),
        NavItem("Kalendarz", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth, Calendar),
        NavItem("Statystyki", Icons.Outlined.Insights, Icons.Filled.Insights, Stats),
        NavItem("Ustawienia", Icons.Outlined.Settings, Icons.Filled.Settings, Settings)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(item.route::class)
                    } == true
                    NavigationBarItem(
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Today,
            modifier = Modifier.padding(padding)
        ) {
            composable<Today> { TodayScreen(scrollState = todayScrollState) }
            composable<Calendar> { CalendarScreen(listState = calendarListState) }
            composable<Stats> { StatsScreen(scrollState = statsScrollState) }
            composable<Settings> { SettingsScreen(scrollState = settingsScrollState) }
        }
    }
}
