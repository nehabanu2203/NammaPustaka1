package com.example.nammapustaka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.FirebaseApp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nammapustaka.ui.screens.*
import com.example.nammapustaka.ui.theme.NammaPustakaTheme
import com.example.nammapustaka.viewmodel.LibraryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Firebase init failed", e)
        }
        enableEdgeToEdge()
        setContent {
            NammaPustakaTheme {
                MainApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Welcome", Icons.Default.Home)
    object Home : Screen("home", "Books", Icons.Default.Home)
    object Scan : Screen("scan", "Scan", Icons.Default.Search)
    object History : Screen("history", "History", Icons.Default.DateRange)
    object Students : Screen("students", "Students", Icons.Default.Person)
    object Leaderboard : Screen("leaderboard", "Ranking", Icons.Default.List)
    object AI : Screen("ai_assistant", "AI", Icons.Default.AutoAwesome)
    object Admin : Screen("admin", "Admin", Icons.Default.Assessment)
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val viewModel: LibraryViewModel = viewModel()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route != Screen.Splash.route

    val items = listOf(
        Screen.Home,
        Screen.Scan,
        Screen.History,
        Screen.AI,
        Screen.Admin
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    tonalElevation = 8.dp
                ) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp))
        ) {
            composable(Screen.Splash.route) { SplashScreen(navController) }
            composable(Screen.Home.route) { HomeScreen(viewModel, navController) }
            composable(Screen.Scan.route) { ScannerScreen(viewModel, navController) }
            composable(Screen.History.route) { HistoryScreen(viewModel) }
            composable(Screen.Students.route) { StudentScreen(viewModel, navController) }
            composable(Screen.Leaderboard.route) { LeaderboardScreen(viewModel) }
            composable(Screen.AI.route) { AIAssistantScreen(navController) }
            composable(Screen.Admin.route) { DashboardScreen(viewModel, navController) }
            composable("add_book") { AddBookScreen(viewModel, navController) }
            composable(
                route = "book_detail/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                BookDetailScreen(bookId, viewModel, navController)
            }
        }
    }
}
