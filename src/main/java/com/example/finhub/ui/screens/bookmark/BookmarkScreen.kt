package com.example.finhub.ui.screens.bookmark

import android.annotation.SuppressLint
import android.widget.Toast
import com.example.finhub.utils.NotificationManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finhub.ui.screens.home.NewsCard
import com.example.finhub.ui.screens.home.DevBytesTheme
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import com.example.finhub.viewmodel.BookmarkViewModel
import com.example.finhub.ui.screens.home.SideMenu
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.graphics.Color
import com.example.finhub.ui.theme.Gray
import com.example.finhub.ui.theme.HomeBackgroundTheme

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarkScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val viewModel: BookmarkViewModel = viewModel(
        factory = BookmarkViewModel.provideFactory(context)
    )
    val bookmarks by viewModel.bookmarks.collectAsState()

    // Add notification host to display notifications
    NotificationManager.NotificationHost()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = HomeBackgroundTheme)
    ){
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Saved Items",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.zIndex(1f)
                )
            },
            containerColor = Color.Transparent // Same dark background for the whole screen
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = Color.Transparent // Match the dark background
            ) {
                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved items yet",
                            color = Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { bookmarks.size })
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            pageSize = PageSize.Fill,
                            contentPadding = PaddingValues(0.dp),
                            pageSpacing = 0.dp
                        ) { page ->
                            val article = bookmarks[page]
                            val pageOffset = calculatePageOffset(pagerState, page)

                            NewsCard(
                                article = article,
                                onClick = {
                                    navController.navigate(
                                        "articleDetail/${Uri.encode(article.headline)}/${Uri.encode(article.content)}/${Uri.encode(article.image)}/${Uri.encode(article.source)}/${Uri.encode(article.datetime)}/${Uri.encode(article.url)}"
                                    )
                                },
                                onBookmarkClick = {
                                    viewModel.toggleBookmark(article)
                                    NotificationManager.showBookmarkRemoved("Bookmark removed: ${article.headline}")
                                },
                                isBookmarked = true,
                                currentPosition = page + 1,
                                totalCards = bookmarks.size,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationY = pageOffset * size.height * 0.1f
                                        alpha = 1f - abs(pageOffset).coerceIn(0f, 1f)
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

// Utility function for linear interpolation
fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}