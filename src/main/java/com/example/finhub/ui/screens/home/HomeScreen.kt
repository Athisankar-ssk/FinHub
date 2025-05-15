package com.example.finhub.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.finhub.R
import com.example.finhub.data.model.NewsArticle
import com.example.finhub.ui.components.RandomFact
import com.example.finhub.viewmodel.BookmarkViewModel
import com.example.finhub.viewmodel.NewsViewModel
import com.example.finhub.viewmodel.TopicsViewModel
import com.google.firebase.annotations.concurrent.Background
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import com.example.finhub.ui.theme.*
import com.example.finhub.data.database.FirebaseUserService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.finhub.admin.ConfirmationDialog
import com.example.finhub.utils.NotificationManager

// DevBytes-inspired color palette using the provided purple theme
object DevBytesTheme {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Black,
            Color(0xFF0F172A), // slate-900 equivalent
            Color.Black
        )
    )
    val Purple80 = Color(0xFFAE95E8)
    val PurpleGrey80 = Color(0xFFCCC2DC)
    val Pink80 = Color(0xFFEFB8C8)

    val Purple40 = Color(0xFF3C1C87)
    val PurpleGrey40 = Color(0xFF625b71)
    val Pink40 = Color(0xFF7D5260)

    val primaryColor = Purple40
    val secondaryColor = PurpleGrey40
    val accentColor = Pink40

    val darkBackground = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val textPrimary = Color.White
    val textSecondary = Color(0xFFCCCCCC)
}

@Composable
fun TopNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    followedTopics: List<String>,
    onAddTopicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Daily", "My Feed") + followedTopics

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
            containerColor = Color.Transparent,
            contentColor = Color.LightGray,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 48.dp),
            divider = {},
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(
                            tabPositions[tabs.indexOf(selectedTab).coerceAtLeast(0)]
                        )
                        .height(3.dp)
                        .background(MediumVilot)
                )
            }

        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab,
                            color = if (selectedTab == tab) DevBytesTheme.textPrimary else DevBytesTheme.textSecondary,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            maxLines = 1
                        )
                    }
                )
            }
        }

        // Fixed position Add button
        IconButton(
            onClick = onAddTopicClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(
                    color = backgroundDarkColor,  // Dark purple background
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add topics",
                tint = MediumVilot  // Light purple icon
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Check if user is authenticated
    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            // Clear shared preferences
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_logged_in", false)
                .apply()

            // Navigate to welcome screen
            navController.navigate("welcome") {
                popUpTo("home") { inclusive = true }
            }
            return@LaunchedEffect
        }
    }

    val newsViewModel: NewsViewModel = viewModel(
        factory = NewsViewModel.provideFactory(context)
    )
    val bookmarkViewModel: BookmarkViewModel = viewModel(
        factory = BookmarkViewModel.provideFactory(context)
    )
    val topicsViewModel: TopicsViewModel = viewModel(
        factory = TopicsViewModel.provideFactory()
    )

    val newsArticles by newsViewModel.newsArticles.collectAsState()
    val followedTopics by topicsViewModel.followedTopics.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // State for selected tab
    var selectedTab by remember { mutableStateOf("Daily") }
    var isInitialLoading by remember { mutableStateOf(true) }
    var userInterests by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Collect user interests flow
    LaunchedEffect(Unit) {
        FirebaseUserService.getUserInterestsFlow().collect { interests ->
            userInterests = interests
            isLoading = false
        }
    }

    // Effect to handle loading state
    LaunchedEffect(newsArticles) {
        if (newsArticles.isNotEmpty()) {
            isInitialLoading = false
        }
    }

    // Effect to refresh topics when coming back from PersonalizeFeed
    LaunchedEffect(Unit) {
        topicsViewModel.refreshTopics()
    }

    // Utility functions for date handling
    fun getCurrentDateInUTC(): String {
        return LocalDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    fun getYesterdayDateInUTC(): String {
        return LocalDateTime.now(ZoneOffset.UTC)
            .minusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    // Add notification host to display notifications
    NotificationManager.NotificationHost()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            if (drawerState.isOpen) {
                SideMenu(
                    navController = navController,
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackgroundTheme)
    ) {
        Scaffold(
            topBar = {

                Column {
                    TopAppBar(
                        title = {
                            Text(
                                "FinHub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = DevBytesTheme.textPrimary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = "Menu",
                                    tint = OnboardingTextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = DevBytesTheme.textPrimary,
                            navigationIconContentColor = OnboardingTextSecondary
                        ),
                        modifier = Modifier.zIndex(1f)
                    )

                    TopNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { tab -> selectedTab = tab },
                        followedTopics = followedTopics,
                        onAddTopicClick = {
                            navController.navigate("personalize_feed") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (isInitialLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
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
                        val loadingFact by remember { mutableStateOf(RandomFact()) }
                        Text(
                            text = loadingFact,
                            fontSize = 18.sp,
                            color = OnboardingTextSecondary,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(start = 32.dp, end = 32.dp)
                        )
                    }
                }
            } else {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentPurple)
                    }
                } else {
                    // Create tabs based on interests
                    val tabs = remember(userInterests) {
                        listOf("For You") + userInterests
                    }

                    val filteredArticles = when (selectedTab) {
                        "Daily" -> {
                            // First get daily category article
                            val dailyArticles = newsArticles.filter {
                                it.category == "daily finance" ||
                                        it.category == "daily business"
                            }

                            // Get today's articles
                            val todayArticles = dailyArticles.filter {
                                it.savedDate.startsWith(getCurrentDateInUTC())
                            }

                            // If no today's articles, get yesterday's
                            val filteredDailyArticles = if (todayArticles.isEmpty()) {
                                dailyArticles.filter {
                                    it.savedDate.startsWith(getYesterdayDateInUTC())
                                }
                            } else {
                                todayArticles
                            }

                            // Sort by datetime in descending order
                            filteredDailyArticles.sortedByDescending { it.datetime }
                        }

                        "My Feed" -> {
                            // Get lowercase list of followed topics
                            val followedCategories = followedTopics.map { it.lowercase() }

                            // Get articles that would appear in Daily tab (today and yesterday's daily news)
                            val dailyArticles = newsArticles.filter {
                                (it.category == "daily finance" || it.category == "daily business") &&
                                        (it.savedDate.startsWith(getCurrentDateInUTC()) ||
                                                it.savedDate.startsWith(getYesterdayDateInUTC()))
                            }

                            // Filter and sort by datetime in descending order
                            newsArticles.filter { article ->
                                val category = article.category.lowercase()
                                !followedCategories.contains(category) &&
                                        !dailyArticles.contains(article)
                            }.sortedByDescending { it.datetime }
                        }

                        else -> {
                            // Filter by topic and sort by datetime in descending order
                            newsArticles.filter { article ->
                                article.category.lowercase() == selectedTab.lowercase() ||
                                        article.category.lowercase()
                                            .contains(selectedTab.lowercase())
                            }.sortedByDescending { it.datetime }
                        }
                    }

                    if (filteredArticles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    "My Feed" -> "No general news available"
                                    else -> "No news available for $selectedTab"
                                },
                                color = DevBytesTheme.textSecondary,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        val pagerState = rememberPagerState(pageCount = { filteredArticles.size })
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(Color.Transparent)
                        ) {
                            VerticalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                pageSize = PageSize.Fill,
                                contentPadding = PaddingValues(0.dp),
                                pageSpacing = 0.dp
                            ) { page ->
                                val article = filteredArticles[page]
                                val pageOffset = calculatePageOffset(pagerState, page)

                                NewsCard(
                                    article = article,
                                    onClick = {
                                        try {
                                            if (article.headline.isBlank()) {
                                                NotificationManager.showError("Article headline is missing")
                                                return@NewsCard
                                            }

                                            val route = "articleDetail/" +
                                                    Uri.encode(article.headline) + "/" +
                                                    Uri.encode(article.content ?: "") + "/" +
                                                    Uri.encode(article.image ?: "") + "/" +
                                                    Uri.encode(article.source ?: "") + "/" +
                                                    Uri.encode(article.datetime) + "/" +
                                                    Uri.encode(article.url)

                                            navController.navigate(route) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        } catch (e: Exception) {
                                            Log.e("HomeScreen", "Navigation error: ${e.message}", e)
                                            NotificationManager.showError("Error opening article: ${e.message}")
                                        }
                                    },
                                    onBookmarkClick = {
                                        bookmarkViewModel.toggleBookmark(article)
                                        if (!bookmarkViewModel.isArticleBookmarked(article.url ?: "")) {
                                            NotificationManager.showBookmarked("Bookmarked: ${article.headline}")
                                        } else {
                                            NotificationManager.showBookmarkRemoved("Bookmark removed: ${article.headline}")
                                        }
                                    },
                                    isBookmarked = bookmarkViewModel.isArticleBookmarked(
                                        article.url ?: ""
                                    ),
                                    currentPosition = page + 1,
                                    totalCards = filteredArticles.size,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            translationY = pageOffset * size.height * 0.1f
                                            scaleX = lerp(
                                                start = 0.85f,
                                                stop = 1f,
                                                fraction = 1f - abs(pageOffset).coerceIn(0f, 1f)
                                            )
                                            scaleY = lerp(
                                                start = 0.85f,
                                                stop = 1f,
                                                fraction = 1f - abs(pageOffset).coerceIn(0f, 1f)
                                            )
                                            alpha = lerp(
                                                start = 0.5f,
                                                stop = 1f,
                                                fraction = 1f - abs(pageOffset).coerceIn(0f, 1f)
                                            )
                                            rotationX = pageOffset * 15f
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom function to calculate page offset
fun calculatePageOffset(pagerState: PagerState, page: Int): Float {
    val currentPage = pagerState.currentPage
    val offset = pagerState.currentPageOffsetFraction
    return when {
        page < currentPage -> -1f + offset
        page > currentPage -> 1f + offset
        else -> offset
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideMenu(navController: NavController, onClose: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // State for showing the logout confirmation dialog
    var showDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }

    // Check if user is not authenticated
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            // Clear shared preferences
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_logged_in", false)
                .apply()

            // Navigate to welcome screen
            navController.navigate("welcome") {
                popUpTo("home") { inclusive = true }
            }
            return@LaunchedEffect
        }
    }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxSize(),
        drawerContainerColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SideBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Header with FinHub and close arrow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .background(Color.Transparent),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FinHub",
                        color = DevBytesTheme.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Close Menu",
                            tint = DevBytesTheme.textPrimary
                        )
                    }
                }

                // User Profile Section
                if (currentUser != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .border(
                                    width = 2.dp,
                                    color = Follow,
                                    shape = CircleShape
                                )
                                .background(
                                    color = Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser.displayName?.firstOrNull() ?: "U").toString(),
                                color = OnboardingTextSecondary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Name and Email
                        Column {
                            Text(
                                text = currentUser.displayName ?: "User",
                                color = DevBytesTheme.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentUser.email ?: "No Email",
                                color = OnboardingTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Divider(
                    color = OnboardingTextSecondary,
                    thickness = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth(1.0f)
                        .padding(vertical = 20.dp)
                )

                // PERSONAL Section
                Text(
                    text = "PERSONAL",
                    color = OnboardingTextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp, bottom = 16.dp)
                )

                // Menu Items in Cards
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        contentColor = OnboardingTextSecondary
                    ),
                    border = BorderStroke(1.dp, OnboardingTextSecondary),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    SideMenuItem(
                        title = "Today's Story",
                        icon = Icons.Default.Article,
                        onClick = {
                            navController.navigate("today_story")
                            onClose()
                        }
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        contentColor = OnboardingTextSecondary
                    ),
                    border = BorderStroke(1.dp, OnboardingTextSecondary),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    SideMenuItem(
                        title = "Saved Items",
                        icon = Icons.Default.Bookmarks,
                        onClick = {
                            navController.navigate("bookmarks")
                            onClose()
                        }
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        contentColor = OnboardingTextSecondary
                    ),
                    border = BorderStroke(1.dp, OnboardingTextSecondary),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    SideMenuItem(
                        title = "Personalize Feed",
                        icon = Icons.Default.Person,
                        onClick = {
                            navController.navigate("personalize_feed")
                            onClose()
                        }
                    )
                }

                Text(
                    text = "APP INFO",
                    color = OnboardingTextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp, top = 16.dp )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    BottomTextLink(text = "About ") { showAboutDialog = true }
                    BottomTextLink(text = "Contact Us ") { showContactDialog = true }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Logout Button
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Follow,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = DevBytesTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    }
                }


            }
        }
    }

    // Logout Confirmation Bottom Sheet/Dialog
    ConfirmationDialog(
        isVisible = showDialog,
        title = "Are you sure you want to logout?",
        confirmButtonText = "Yes, logout",
        cancelButtonText = "Cancel",
        onConfirm = {
            auth.signOut()
            sharedPreferences.edit().putBoolean("is_logged_in", false).apply()
            navController.navigate("welcome") {
                popUpTo("home") { inclusive = true }
            }
            onClose()
            showDialog = false },
        onCancel = { showDialog = false }
    )


    // About and Contact Us dialogs
    if (showAboutDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAboutDialog = false },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            containerColor = Color.Transparent,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SideBackground)
                    .padding(24.dp)
            ) {
                // Header with back button and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAboutDialog = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = DevBytesTheme.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "About FinHub",
                        color = DevBytesTheme.textPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    val sentences = listOf(
                        "FinHub is an AI-powered finance and business news application designed to simplify how users stay informed about the economy, markets, and industry trends. Our mission is to deliver concise, relevant, and reliable financial insights to everyone — from curious learners to active investors — all in under a minute.",
                        "We understand that time is valuable. That's why FinHub presents news in a modern, swipeable card format, allowing users to browse through important updates quickly and efficiently. Each story is summarized using AI, highlighting the key points so you can stay informed without spending too much time.",
                        "FinHub adapts to your interests. With a personalized feed, users can select their preferred topics, which are then automatically featured in a top scrollable tab — ensuring the most relevant news is always within easy reach.",
                        "In addition to regular updates, FinHub includes a special feature called \"One Story, Every Day\", showcasing the inspiring journey of a prominent business leader. It's designed to provide daily motivation and deeper insight into the world of entrepreneurship and leadership.",
                        "Users can also save news cards for offline reading, share news cards effortlessly, and explore each topic further with a \"Read Full Article\" option. These full articles are written in simplified language for better understanding and include a link to the original source for full transparency.",
                        "FinHub is more than a news app — it's your intelligent, personalized companion for staying business-minded and financially informed and inspired, every day."
                    )
                    sentences.forEach { sentence ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top

                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.chip),
                                contentDescription = "Bullet Point",
                                tint = Purple80,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(top = 4.dp)
                            )
//
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sentence,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp
                                ),
                                color = OnboardingTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Add some padding at the bottom for better scrolling experience
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    if (showContactDialog) {
        ModalBottomSheet(
            onDismissRequest = { showContactDialog = false },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            containerColor = Color.Transparent,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SideBackground)
                    .padding(24.dp)
            ) {
                // Header with back button and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showContactDialog = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = DevBytesTheme.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Contact Us",
                        color = DevBytesTheme.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )

                }

                Spacer(modifier = Modifier.height(32.dp))

                // DevBytes Logo as Text
                Text(
                    text = "FinHub",
                    color = DevBytesTheme.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Head Office Section
                Text(
                    text = "Head Office",
                    color = DevBytesTheme.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FinHub Software Solutions",
                    color = DevBytesTheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "95, Avinash Road, Peelamedu\nCoimbatore, Tamil Nadu 641001",
                    color = OnboardingTextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email Section
                Text(
                    text = "Email",
                    color = DevBytesTheme.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "24mx104@psgtech.ac.in ↗",
                    color = MediumVilot,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:24mx104@psgtech.ac.in")
                        }
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "24mx217@psgtech.ac.in ↗",
                    color = MediumVilot,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:24mx217@psgtech.ac.in")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun SideMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = DevBytesTheme.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = DevBytesTheme.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = DevBytesTheme.textSecondary
        )
    }
}

@Composable
private fun SwipeProgressLine(
    currentPosition: Int,
    totalCards: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.Gray.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(currentPosition.toFloat() / totalCards.coerceAtLeast(1))
                .height(2.dp)
                .background(MediumVilot)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NewsCard(
    article: NewsArticle,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    isBookmarked: Boolean,
    currentPosition: Int,
    totalCards: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
        shape = RoundedCornerShape(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Content section with weight to push bottom row down
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (article.image.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp, 5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BlueViolet.copy(alpha = 0.8f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .align(Alignment.BottomStart)
                        ) {
                            Text(
                                text = article.source,
                                color = DevBytesTheme.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp, 5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BlueViolet.copy(alpha = 0.8f))
                                .padding(horizontal = 4.dp, vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, article.headline)
                                            putExtra(
                                                Intent.EXTRA_TEXT, """
                                            ${article.headline}
                                            
                                            ${article.summary}
                                            
                                            Read more at: ${article.url}
                                            
                                            Source: ${article.source}
                                        """.trimIndent()
                                            )
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                shareIntent,
                                                "Share FinHub News Card Via"
                                            )
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = DevBytesTheme.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onBookmarkClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.Bookmark,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) Melrose else DevBytesTheme.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = article.headline,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                lineHeight = 32.sp
                            ),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            color = DevBytesTheme.textPrimary
                        )

                        val sentences = article.summary.split(Regex("(?<=[.!?])\\s+(?=[A-Z])"))
                            .filter { it.trim().isNotEmpty() }
                            .map { it.trim() }

                        sentences.forEach { sentence ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top

                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.chip),
                                    contentDescription = "Bullet Point",
                                    tint = Purple80,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(top = 4.dp)
                                )
//
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sentence,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 18.sp,
                                        lineHeight = 22.sp
                                    ),
                                    color = OnboardingTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        AsyncImage(
                            model = article.image,
                            contentDescription = article.headline,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            SemiTransparentBlack
                                        ),
                                        startY = 300f,
                                        endY = 600f
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .padding(16.dp, 5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BlueViolet.copy(alpha = 0.8f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .align(Alignment.BottomStart)
                        ) {
                            Text(
                                text = article.source,
                                color = DevBytesTheme.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp, 5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BlueViolet.copy(alpha = 0.8f))
                                .padding(horizontal = 4.dp, vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, article.headline)
                                            putExtra(
                                                Intent.EXTRA_TEXT, """
                                            ${article.headline}
                                            
                                            ${article.summary}
                                            
                                            Read more at: ${article.url}
                                            
                                            Source: ${article.source}
                                        """.trimIndent()
                                            )
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                shareIntent,
                                                "Share FinHub News Card Via"
                                            )
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = DevBytesTheme.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onBookmarkClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.Bookmark,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) Melrose else DevBytesTheme.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color.Transparent)
                    ) {
                        Text(
                            text = article.headline,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                lineHeight = 28.sp
                            ),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            color = DevBytesTheme.textPrimary
                        )

                        Text(
                            text = article.summary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 18.sp,
                                lineHeight = 24.sp
                            ),
                            color = OnboardingTextSecondary
                        )
                    }
                }
            }

            // Bottom section that will always be at the end
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Bottom row with source and action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Source with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Globe icon
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Transparent)
                                .padding(0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = "Source",
                                tint = MediumVilot,
                                modifier = Modifier.size(45.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column {
                            Text(
                                text = article.source ?: "News",
                                color = DevBytesTheme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onClick() }
                            ) {
                                Text(
                                    text = "Read Full Article",
                                    color = OnboardingTextSecondary.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open in browser",
                                    tint = OnboardingTextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Action buttons row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            val formattedTime = try {
                                val input = article.datetime // example: "2025-05-09"
                                val date = LocalDate.parse(
                                    input,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                )
                                val today = LocalDate.now()
                                val yesterday = today.minusDays(1)
                                when (date) {
                                    today -> "Today"
                                    yesterday -> "Yesterday"
                                    else -> date.format(
                                        DateTimeFormatter.ofPattern(
                                            "MMM d, yyyy",
                                            Locale.ENGLISH
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                "\uD83D\uDCC6"
                            }

                            Text(
                                text = formattedTime,
                                color = OnboardingTextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Progress line at the very bottom
                SwipeProgressLine(
                    currentPosition = currentPosition,
                    totalCards = totalCards,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@Composable
fun BottomTextLink(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = DevBytesTheme.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = Icons.Default.ArrowOutward,
            contentDescription = null,
            tint = DevBytesTheme.textSecondary,
            modifier = Modifier
                .size(16.dp)
        )
    }
}