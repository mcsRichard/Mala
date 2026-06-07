package com.meritminder.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.ui.auth.AuthViewModel
import com.meritminder.app.ui.auth.LoginScreen
import com.meritminder.app.ui.auth.RegisterScreen
import com.meritminder.app.ui.counter.CounterScreen
import com.meritminder.app.ui.library.PracticeLibraryScreen
import com.meritminder.app.ui.main.MainScreen
import com.meritminder.app.ui.practice.AddPracticeScreen
import com.meritminder.app.ui.settings.ReminderSettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "main" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                navController = navController,
                authViewModel = authViewModel,
                onOpenCounter = { practiceId -> navController.navigate("counter/$practiceId") }
            )
        }
        composable("practice_library") {
            PracticeLibraryScreen(
                onSelectTemplate = { name ->
                    navController.navigate("add_practice?name=${Uri.encode(name)}")
                },
                onCustomInput = { navController.navigate("add_practice") },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "add_practice?name={name}",
            arguments = listOf(navArgument("name") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) {
            AddPracticeScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = "counter/{practiceId}",
            arguments = listOf(navArgument("practiceId") { type = NavType.IntType })
        ) {
            CounterScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("reminder_settings") {
            ReminderSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
