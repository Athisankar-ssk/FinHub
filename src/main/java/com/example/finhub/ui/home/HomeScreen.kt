package com.example.finhub.ui.home

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.finhub.data.network.NewsArticle
import com.example.finhub.ui.bookmark.BookmarkViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.math.abs

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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(finnhubApiKey: String, newsApiKey: String, navController: NavHostController) {
    val context = LocalContext.current
    val newsViewModel: NewsViewModel = viewModel(
        factory = NewsViewModel.provideFactory(finnhubApiKey, newsApiKey, context)
    )
    val bookmarkViewModel: BookmarkViewModel = viewModel(
        factory = BookmarkViewModel.provideFactory(context)
    )
    val newsArticles by newsViewModel.newsArticles.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            if (drawerState.isOpen) {
                SideMenu(
                    navController = navController,
                    onClose = { scope.launch { drawerState.close() } })
            }
        }
    ) {
        Scaffold(
            topBar = {
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
                val pagerState = rememberPagerState(pageCount = { newsArticles.size })
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
                        val article = newsArticles[page]
                        val pageOffset = calculatePageOffset(pagerState, page)

                        NewsCard(
                            article = article,
                            onClick = {
                                try {
                                    // Validate article data
                                    if (article.headline.isBlank()) {
                                        Toast.makeText(context, "Article headline is missing", Toast.LENGTH_SHORT).show()
                                        return@NewsCard
                                    }

                                    // Create the route with proper encoding and null handling
                                    val route = "articleDetail/" +
                                        Uri.encode(article.headline) + "/" +
                                        Uri.encode(article.content ?: "") + "/" +
                                        Uri.encode(article.image ?: "") + "/" +
                                        Uri.encode(article.source ?: "") + "/" +
                                        Uri.encode(article.datetime)

                                    // Navigate with proper error handling
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
                            totalCards = newsArticles.size,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // Enhanced animation for DevBytes-like swipe
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FinHub",
                    color = DevBytesTheme.Purple80,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (currentUser != null) {
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
                Spacer(modifier = Modifier.height(24.dp))
            }
            Divider(color = DevBytesTheme.textSecondary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))
            SideMenuItem(
                title = "Daily Digest",
                icon = Icons.Filled.Star,
                textColor = DevBytesTheme.textPrimary,
                onClick = { /* Navigate or handle */ }
            )
            SideMenuItem(
                title = "Bookmark",
                icon = Icons.Filled.Bookmark,
                textColor = DevBytesTheme.textPrimary,
                onClick = { /* Navigate or handle */ }
            )
            SideMenuItem(
                title = "Settings",
                icon = Icons.Filled.Settings,
                textColor = DevBytesTheme.textPrimary,
                onClick = { /* Navigate or handle */ }
            )
            Spacer(modifier = Modifier.weight(1f))
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
                    containerColor = DevBytesTheme.Purple40,
                    contentColor = DevBytesTheme.textPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Log Out", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SideMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = DevBytesTheme.Purple80,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
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
