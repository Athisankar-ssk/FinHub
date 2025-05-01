package com.example.finhub.ui.bookmark

import android.widget.Toast
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
import com.example.finhub.ui.home.NewsCard
import com.example.finhub.ui.home.DevBytesTheme
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import com.example.finhub.ui.home.SideMenu
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarkScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val viewModel: BookmarkViewModel = viewModel(
        factory = BookmarkViewModel.provideFactory(context)
    )
    val bookmarks by viewModel.bookmarks.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            if (drawerState.isOpen) {
                SideMenu(navController = navController, onClose = { scope.launch { drawerState.close() } })
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Bookmarks",
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
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = DevBytesTheme.darkBackground
            ) {
                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No bookmarked articles",
                            color = DevBytesTheme.textSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { bookmarks.size })
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DevBytesTheme.darkBackground)
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
                                        "articleDetail/${Uri.encode(article.headline)}/${Uri.encode(article.content)}/${Uri.encode(article.image)}/${Uri.encode(article.source)}/${Uri.encode(article.datetime)}"
                                    )
                                },
                                onBookmarkClick = {
                                    viewModel.toggleBookmark(article)
                                    Toast.makeText(
                                        context,
                                        " Bookmark removed \n \"${article.headline}\"",
                                        Toast.LENGTH_SHORT
                                    ).show()
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