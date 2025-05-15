package com.example.finhub.ui.navigation

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.finhub.ui.screens.home.HomeScreen
import com.example.finhub.ui.screens.home.ArticleDetailScreen
import com.example.finhub.ui.screens.home.DevBytesTheme
import com.example.finhub.ui.screens.bookmark.BookmarkScreen
import com.example.finhub.ui.screens.welcome.OnboardingScreen
import com.example.finhub.ui.screens.welcome.SignInScreen
import com.example.finhub.ui.screens.welcome.SignUpScreen
import com.example.finhub.ui.screens.welcome.WelcomeScreen
import com.example.finhub.ui.screens.welcome.InterestSelectionScreen
import com.example.finhub.ui.screens.welcome.PersonalizeFeedScreen
import com.example.finhub.data.model.NewsArticle
import com.example.finhub.ui.components.RandomFact
import com.example.finhub.ui.theme.HomeBackgroundTheme
import com.example.finhub.ui.theme.OnboardingTextSecondary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import com.example.finhub.admin.AdminScreen
import com.example.finhub.data.database.FirebaseAdminService
import com.example.finhub.ui.screens.today.TodayStoryScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(
    shouldOpenAdminNews: Boolean = false,
    adminTab: String = ""
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // State for start destination
    var startDestination by remember { mutableStateOf("loading") }

    // Combined LaunchedEffect for navigation and initialization
    LaunchedEffect(Unit) {
        // Check if onboarding is completed
        val isOnboardingCompleted = sharedPreferences.getBoolean("showOnboarding", true)

        // Validate Firebase user
        val currentUser = try {
            auth.currentUser?.reload()?.await()
            auth.currentUser
        } catch (e: Exception) {
            null
        }

        // Reset is_logged_in if no valid user
        if (currentUser == null) {
            sharedPreferences.edit()
                .putBoolean("is_logged_in", false)
                .apply()
        }

        // Set start destination and handle admin news navigation
        if (shouldOpenAdminNews) {
            startDestination = "admin"
            // Save the tab selection before navigation
            if (adminTab.isNotEmpty()) {
                sharedPreferences.edit()
                    .putString("admin_selected_tab", adminTab)
                    .apply()
            }
        } else {
            // Get the logged in state from SharedPreferences
            val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
            
            startDestination = when {
                !isOnboardingCompleted -> "onboarding"
                FirebaseAdminService.isAdminSession(context) -> "admin"
                currentUser == null -> "welcome"
                !currentUser.isEmailVerified -> {
                    // If email is not verified, make sure logged_in is false and go to signin
                    sharedPreferences.edit()
                        .putBoolean("is_logged_in", false)
                        .apply()
                    "signin"
                }
                !isLoggedIn -> "signin"  // If not logged in (even with verified email), go to signin
                else -> {
                    // Check for interests before going to home
                    val interests = com.example.finhub.data.database.FirebaseUserService.getUserInterests()
                    if (interests.isEmpty()) "interest_selection" else "home"
                }
            }
        }
    }

    Scaffold(
        containerColor = DevBytesTheme.darkBackground,
//        bottomBar = {
//            if (currentRoute in listOf("home", "trending", "markets", "bookmarks")) {
//                BottomNavigationBar(
//                    currentRoute = currentRoute ?: "home",
//                    onNavigate = { route ->
//                        navController.navigate(route) {
//                            popUpTo(navController.graph.startDestinationId) {
//                                saveState = true
//                            }
//                            launchSingleTop = true
//                            restoreState = true
//                        }
//                    }
//                )
//            }
//        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("loading") {
                Box(
                    modifier = Modifier.fillMaxSize().background(HomeBackgroundTheme),
                    contentAlignment = Alignment.Center

                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val composition by rememberLottieComposition(
                            spec = LottieCompositionSpec.Asset("loading_animation.json")
                        )

                        val progress by animateLottieCompositionAsState(
                            composition = composition,
                            iterations = LottieConstants.IterateForever
                        )

                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier.size(100.dp)
                        )
                        val loadingFact = remember { RandomFact() }
                        Text(
                            text = loadingFact,
                            fontSize = 18.sp,
                            color = OnboardingTextSecondary,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(start = 32.dp, end = 32.dp)
                        )
                    }
                }
            }

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

            composable("trending") {
                TrendingScreen()
            }

            composable("markets") {
                MarketsScreen()
            }

            composable("bookmarks") {
                BookmarkScreen(navController)
            }

            composable(
                route = "articleDetail/{headline}/{content}/{imageUrl}/{source}/{datetime}/{url}",
                arguments = listOf(
                    navArgument("headline") { type = NavType.StringType },
                    navArgument("content") { type = NavType.StringType },
                    navArgument("imageUrl") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                    navArgument("datetime") { type = NavType.StringType },
                    navArgument("url") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val headline = backStackEntry.arguments?.getString("headline") ?: ""
                val content = backStackEntry.arguments?.getString("content") ?: ""
                val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                val source = backStackEntry.arguments?.getString("source") ?: ""
                val dateAndTime = backStackEntry.arguments?.getString("datetime") ?: ""
                val url = backStackEntry.arguments?.getString("url") ?: ""

                val article = NewsArticle(
                    headline = headline,
                    content = content,
                    image = imageUrl,
                    source = source,
                    summary = "",
                    datetime = dateAndTime,
                    url = url
                )

                ArticleDetailScreen(article = article)
            }

            composable("interest_selection") {
                InterestSelectionScreen(navController)
            }

            composable("personalize_feed") {
                PersonalizeFeedScreen(navController)
            }

            composable("admin") {
                AdminScreen(navController)
            }

            composable("today_story") {
                TodayStoryScreen(navController)
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
