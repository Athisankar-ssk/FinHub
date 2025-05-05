package com.example.finhub.ui.navigation

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.finhub.ui.screens.home.HomeScreen
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finhub.data.model.NewsArticle
import com.example.finhub.ui.screens.home.ArticleDetailScreen
import com.example.finhub.ui.screens.home.DevBytesTheme
import com.example.finhub.ui.screens.bookmark.BookmarkScreen
import com.example.finhub.ui.screens.welcome.OnboardingScreen
import com.example.finhub.ui.screens.welcome.SignInScreen
import com.example.finhub.ui.screens.welcome.SignUpScreen
import com.example.finhub.ui.screens.welcome.WelcomeScreen
import com.example.finhub.ui.screens.welcome.InterestSelectionScreen
import com.example.finhub.ui.screens.welcome.PersonalizeFeedScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val sharedPreferences = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    val isOnboardingCompleted = remember {
        !sharedPreferences.getBoolean("showOnboarding", true)
    }

    val isUserLoggedIn = remember {
        sharedPreferences.getString("user_token", null) != null
    }

    val startDestination = when {
        !isOnboardingCompleted -> "onboarding"
        !isUserLoggedIn -> "signin"
        else -> "home"
    }

    Scaffold(
        containerColor = DevBytesTheme.darkBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
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

            composable("home") {
                HomeScreen(navController = navController)
            }
            
            composable("bookmarks") {
                BookmarkScreen(navController)
            }

            composable(
                route = "articleDetail/{headline}/{content}/{imageUrl}/{source}/{datetime}",
                arguments = listOf(
                    navArgument("headline") { type = NavType.StringType },
                    navArgument("content") { type = NavType.StringType },
                    navArgument("imageUrl") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                    navArgument("datetime") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val headline = backStackEntry.arguments?.getString("headline") ?: ""
                val content = backStackEntry.arguments?.getString("content") ?: ""
                val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                val source = backStackEntry.arguments?.getString("source") ?: ""
                val dateAndTime = backStackEntry.arguments?.getString("datetime")?: ""

                val article = NewsArticle(
                    headline = headline,
                    content = content,
                    image = imageUrl,
                    source = source,
                    summary = "",
                    datetime = dateAndTime
                )

                ArticleDetailScreen(article = article)
            }

            composable("interest_selection") {
                InterestSelectionScreen(navController) {
                    navController.navigate("home") {
                        popUpTo("interest_selection") { inclusive = true }
                    }
                }
            }

            composable("personalize_feed") {
                PersonalizeFeedScreen(navController)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Trending,
        BottomNavItem.Markets,
        BottomNavItem.Bookmarks
    )

    NavigationBar(
        containerColor = DevBytesTheme.surfaceColor,
        contentColor = DevBytesTheme.textPrimary,
        tonalElevation = 4.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label,
                        tint = if (currentRoute == item.route) DevBytesTheme.Purple80 else DevBytesTheme.textSecondary
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (currentRoute == item.route) DevBytesTheme.Purple80 else DevBytesTheme.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (currentRoute == item.route) FontWeight.Medium else FontWeight.Normal
                    )
                },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DevBytesTheme.Purple80,
                    unselectedIconColor = DevBytesTheme.textSecondary,
                    selectedTextColor = DevBytesTheme.Purple80,
                    unselectedTextColor = DevBytesTheme.textSecondary,
                    indicatorColor = DevBytesTheme.Purple40.copy(alpha = 0.3f)
                )
            )
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
fun BookmarkScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Bookmarked News")
    }
}