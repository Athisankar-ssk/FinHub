package com.example.finhub.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.finhub.data.model.NewsArticle
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
            containerColor = DevBytesTheme.darkBackground,
            contentColor = DevBytesTheme.textPrimary,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 48.dp),
            divider = {}
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
                    color =DevBytesTheme.darkBackground,  // Dark purple background
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add topics",
                tint = Color(0xFF832BFF)  // Light purple icon
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
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
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                TopAppBar(
                    title = {
                        Text(
                            "FinHub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = DevBytesTheme.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = DevBytesTheme.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DevBytesTheme.primaryColor,
                        titleContentColor = DevBytesTheme.textPrimary,
                        navigationIconContentColor = DevBytesTheme.textPrimary
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
            containerColor = DevBytesTheme.darkBackground
        ) { paddingValues ->
            if (newsArticles.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DevBytesTheme.darkBackground
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = DevBytesTheme.Purple80
                        )
                    }
                }
            } else {
                val filteredArticles = when (selectedTab) {
                    "Daily" -> {
                        // First get daily category articles
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
                            article.category.lowercase().contains(selectedTab.lowercase())
                        }.sortedByDescending { it.datetime }
                    }
                }

                if (filteredArticles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(DevBytesTheme.darkBackground),
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
                        .background(DevBytesTheme.darkBackground)
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
                                        Toast.makeText(context, "Article headline is missing", Toast.LENGTH_SHORT).show()
                                        return@NewsCard
                                    }

                                    val route = "articleDetail/" +
                                        Uri.encode(article.headline) + "/" +
                                        Uri.encode(article.content ?: "") + "/" +
                                        Uri.encode(article.image ?: "") + "/" +
                                        Uri.encode(article.source ?: "") + "/" +
                                        Uri.encode(article.datetime)

                                    navController.navigate(route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("HomeScreen", "Navigation error: ${e.message}", e)
                                    Toast.makeText(context, "Error opening article: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBookmarkClick = {
                                bookmarkViewModel.toggleBookmark(article)
                                val message = if (!bookmarkViewModel.isArticleBookmarked(article.url ?: "")) {
                                    " Bookmarked \n \"${article.headline}\""
                                } else {
                                    "  Bookmark removed \n \"${article.headline}\""
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            isBookmarked = bookmarkViewModel.isArticleBookmarked(article.url ?: ""),
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

@Composable
fun SideMenu(navController: NavController, onClose: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    ModalDrawerSheet(
        modifier = Modifier.fillMaxSize(),
        drawerContainerColor = DevBytesTheme.darkBackground
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
                    .padding(bottom = 32.dp),
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
                            .background(
                                color = Color(0xFF6C47FF),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser.displayName?.firstOrNull() ?: "U").toString(),
                            color = DevBytesTheme.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
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
                    color = DevBytesTheme.textSecondary,
                    fontSize = 14.sp
                )
                    }
                }
            }

            // PERSONAL Section
            Text(
                text = "PERSONAL",
                color = DevBytesTheme.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )

            // Menu Items in Cards
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
            SideMenuItem(
                    title = "Saved Items",
                    icon = Icons.Default.Bookmark,
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
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(12.dp)
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

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = {
                    auth.signOut()
                    sharedPreferences.edit().putBoolean("is_logged_in", false).apply()
                    navController.navigate("onboarding") {
                        popUpTo("home") { inclusive = true }
                    }
                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D2350),
                    contentColor = DevBytesTheme.textPrimary
                ),
                shape = RoundedCornerShape(8.dp)
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
                .background(DevBytesTheme.Purple80)
        )
    }
}

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
        color = DevBytesTheme.surfaceColor,
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

                        Spacer(modifier = Modifier.height(16.dp))

                        val sentences = article.summary.split(Regex("(?<=[.!?])\\s+(?=[A-Z])"))
                            .filter { it.trim().isNotEmpty() }
                            .map { it.trim() }

                        sentences.forEach { sentence ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = DevBytesTheme.Purple80,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sentence,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 18.sp,
                                        lineHeight = 22.sp
                                    ),
                                    color = DevBytesTheme.textSecondary
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
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            DevBytesTheme.surfaceColor.copy(alpha = 0.2f)
                                        ),
                                        startY = 550f,
                                        endY = 600f
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .padding(16.dp, 5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DevBytesTheme.Purple40)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .align(Alignment.BottomStart)
                        ) {
                            Text(
                                text = article.source,
                                color = DevBytesTheme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
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
                            color = DevBytesTheme.textSecondary
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
                                .size(48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(DevBytesTheme.surfaceColor.copy(alpha = 0.5f))
                                .padding(0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = "Source",
                                tint = DevBytesTheme.Purple80,
                                modifier = Modifier.size(45.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column {
                            Text(
                                text = article.source ?: "News",
                                color = DevBytesTheme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onClick() }
                            ) {
                                Text(
                                    text = "Read Full Article",
                                    color = DevBytesTheme.textSecondary.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open in browser",
                                    tint = DevBytesTheme.textSecondary.copy(alpha = 0.7f),
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, article.headline)
                                        putExtra(Intent.EXTRA_TEXT, """
                                            ${article.headline}
                                            
                                            ${article.summary}
                                            
                                            Read more at: ${article.url}
                                            
                                            Source: ${article.source}
                                        """.trimIndent())
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = DevBytesTheme.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onBookmarkClick() }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.Bookmark,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) DevBytesTheme.Purple80 else DevBytesTheme.textPrimary,
                                modifier = Modifier.size(18.dp)
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
