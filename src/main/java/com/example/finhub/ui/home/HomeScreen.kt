package com.example.finhub.ui.home

import android.content.Context
import android.widget.Toast
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
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import android.content.SharedPreferences


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(apiKey: String, navController: NavHostController) {
    val viewModelFactory = remember { NewsViewModelFactory(apiKey) }
    val newsViewModel: NewsViewModel = viewModel(factory = viewModelFactory)
    val newsArticles by newsViewModel.newsArticles.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true, // Enable swipe gestures for opening/closing the drawer
        drawerContent = {
            if (drawerState.isOpen) {
                SideMenu(navController = navController, onClose = { scope.launch { drawerState.close() } })
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
fun SideMenu(navController: NavHostController, onClose: () -> Unit) {
    // Get the current user from Firebase
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Get the context and shared preferences
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    // Fetch the home page's background and text color
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurface

    ModalDrawerSheet(
        modifier = Modifier.fillMaxSize(),
        drawerContainerColor = backgroundColor // Using the same background color as the home page
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClose() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Close", tint = textColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore App", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display User Info (Name and Email)
            if (currentUser != null) {
                Text(
                    text = "Hello, ${currentUser.displayName ?: "User"}",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentUser.email ?: "No Email",
                    color = secondaryTextColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Menu items
            Spacer(modifier = Modifier.height(16.dp))

            SideMenuItem("Daily Digest", textColor)
            SideMenuItem("Saved Items", textColor)
            SideMenuItem("Personalize Feed", textColor)

            Spacer(modifier = Modifier.height(16.dp))
            Text("More", color = secondaryTextColor, fontSize = 14.sp)
            SideMenuItem("Apply Now", textColor)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Settings", color = secondaryTextColor, fontSize = 14.sp)
            SideMenuItem("Feed Gesture", textColor)
            SideMenuItem("Notifications", textColor)
            SideMenuItem("Appearance", textColor)

            // Add logout button at the bottom
            Spacer(modifier = Modifier.weight(1f)) // Push logout button to the bottom
            Button(
                onClick = {
                    // Log out the user
                    FirebaseAuth.getInstance().signOut()  // Ensure Firebase sign-out is performed
                    sharedPreferences.edit().putBoolean("is_logged_in", false).apply()

                    // Navigate to the OnboardingScreen instead of exiting the app
                    navController.navigate("onboarding") {
                        popUpTo("home") { inclusive = true }  // Remove "home" from back stack
                    }

                    Toast.makeText(context, "Logged out Successfully", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF2CDCBB)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2CDCBB))
            ) {
                Text(text = "Logout", color = Color.White)
            }
        }
    }
}

@Composable
fun SideMenuItem(title: String, textColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { }
            .background(MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(title, color = textColor, fontSize = 16.sp)
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
