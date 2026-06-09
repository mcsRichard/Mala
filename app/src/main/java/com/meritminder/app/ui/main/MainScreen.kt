package com.meritminder.app.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.meritminder.app.R
import com.meritminder.app.ui.auth.AuthViewModel
import com.meritminder.app.ui.group.GroupScreen
import com.meritminder.app.ui.home.HomeScreen
import com.meritminder.app.ui.practice.PracticeListScreen
import com.meritminder.app.ui.profile.ProfileScreen
import com.meritminder.app.ui.stats.StatisticsScreen

private enum class Tab(val icon: ImageVector, val labelRes: Int) {
    TODAY(Icons.Default.Home, R.string.tab_today),
    PRACTICE(Icons.Default.List, R.string.tab_practice),
    GROUP(Icons.Default.Group, R.string.tab_group),
    STATS(Icons.Default.BarChart, R.string.tab_stats),
    PROFILE(Icons.Default.Person, R.string.tab_profile)
}

@Composable
fun MainScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    onOpenCounter: (Int) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == Tab.PRACTICE) {
                FloatingActionButton(onClick = { navController.navigate("practice_library") }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (selectedTab) {
            Tab.TODAY -> HomeScreen(
                contentPadding = innerPadding,
                snackbarHostState = snackbarHostState,
                onOpenCounter = onOpenCounter
            )
            Tab.PRACTICE -> PracticeListScreen(contentPadding = innerPadding)
            Tab.GROUP -> GroupScreen(contentPadding = innerPadding)
            Tab.STATS -> StatisticsScreen(contentPadding = innerPadding)
            Tab.PROFILE -> ProfileScreen(
                contentPadding = innerPadding,
                onNavigateToSettings = { navController.navigate("reminder_settings") },
                onNavigateToTransmissions = { navController.navigate("transmissions") },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
