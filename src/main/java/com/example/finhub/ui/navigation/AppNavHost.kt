package com.example.finhub.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.finhub.ui.components.BottomNavigationBar
import com.example.finhub.ui.home.HomeScreen
import com.example.finhub.ui.welcome.*


@Composable
fun AppNavHost(apiKey: String) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val sharedPreferences = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    // Check if onboarding was shown and user is logged in
    val isOnboardingCompleted = remember {
        !sharedPreferences.getBoolean("showOnboarding", true)
    }

    val isUserLoggedIn = remember {
        // Example: Check if user token exists
        sharedPreferences.getString("user_token", null) != null
    }

    val startDestination = when {
        !isOnboardingCompleted -> "onboarding"
        !isUserLoggedIn -> "welcome"
        else -> BottomNavItem.Home.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in listOf(
                    BottomNavItem.Home.route,
                    BottomNavItem.Trending.route,
                    BottomNavItem.Markets.route,
                    BottomNavItem.Bookmark.route
                )
            ) {
                BottomNavigationBar(
                    currentRoute = currentRoute ?: BottomNavItem.Home.route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Onboarding and Auth Screens
            composable("onboarding") {
                OnboardingScreen(navController, context)
            }
            composable("welcome") {
                WelcomeScreen(navController)
            }
            composable("signin") {
                SignInScreen(navController)
            }
            composable("signup") {
                SignUpScreen(navController)
            }

            // Main Screens (after login)
            composable(BottomNavItem.Home.route) {
                HomeScreen(apiKey = apiKey)
            }
            composable(BottomNavItem.Trending.route) {
                TrendingScreen()
            }
            composable(BottomNavItem.Markets.route) {
                MarketsScreen()
            }
            composable(BottomNavItem.Bookmark.route) {
                BookmarkScreen()
            }
        }
    }
}

@Composable
fun TrendingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Trending News")
    }
}

@Composable
fun MarketsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Markets Overview")
    }
}

@Composable
fun BookmarkScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Bookmarked News")
    }
}
