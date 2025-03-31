package com.example.finhub.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finhub.data.model.NewsArticle
import com.example.finhub.ui.home.NewsViewModel
import com.example.finhub.ui.home.NewsViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(apiKey: String) {
    val viewModelFactory = remember { NewsViewModelFactory(apiKey) }
    val newsViewModel: NewsViewModel = viewModel(factory = viewModelFactory)
    val newsArticles by newsViewModel.newsArticles.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false, // Ensures the menu is fully hidden until clicked
        drawerContent = {
            if (drawerState.isOpen) { // Only show when explicitly opened
                SideMenu { scope.launch { drawerState.close() } }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FinHub", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (newsArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                NewsPager(newsArticles, paddingValues)
            }
        }
    }
}

@Composable
fun SideMenu(onClose: () -> Unit) {
    // Get the current user from Firebase
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    ModalDrawerSheet(
        modifier = Modifier.fillMaxSize(),
        drawerContainerColor = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Display User Info (Name and Email)
            if (currentUser != null) {
                Text(
                    text = "Hello, ${currentUser.displayName ?: "User"}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentUser.email ?: "No Email",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Menu items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClose() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Close", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore App", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SideMenuItem("Daily Digest")
            SideMenuItem("Saved Items")
            SideMenuItem("Personalize Feed")

            Spacer(modifier = Modifier.height(16.dp))
            Text("More", color = Color.Gray, fontSize = 14.sp)
            SideMenuItem("Apply Now")

            Spacer(modifier = Modifier.height(16.dp))
            Text("Settings", color = Color.Gray, fontSize = 14.sp)
            SideMenuItem("Feed Gesture")
            SideMenuItem("Notifications")
            SideMenuItem("Appearance")

            // Add logout button at the bottom
            Spacer(modifier = Modifier.weight(1f)) // Push logout button to the bottom
            Button(
                onClick = {
                    // Log out the user
                    auth.signOut()
                    // Optionally, navigate to the login screen
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Red),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(text = "Logout", color = Color.White)
            }
        }
    }
}

@Composable
fun SideMenuItem(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { }
            .background(Color.DarkGray, shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun NewsPager(articles: List<NewsArticle>, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        items(articles) { article ->
            NewsCard(article)
        }
    }
}

@Composable
fun NewsCard(article: NewsArticle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = article.headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Source: ${article.source}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

