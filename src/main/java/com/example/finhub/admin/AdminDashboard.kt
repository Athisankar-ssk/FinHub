package com.example.finhub.admin

import android.widget.Toast
import com.example.finhub.utils.NotificationManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.finhub.ui.components.RandomFact
import com.example.finhub.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun AdminDashboard() {
    var totalUsers by remember { mutableStateOf(0) }
    var totalArticles by remember { mutableStateOf(0) }
    var categoryStats by remember { mutableStateOf(mapOf<String, Int>()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Add notification host to display notifications
    NotificationManager.NotificationHost()

    // Track if both listeners have received initial data
    var usersLoaded by remember { mutableStateOf(false) }
    var articlesLoaded by remember { mutableStateOf(false) }

    // Update loading state when both listeners are ready
    LaunchedEffect(usersLoaded, articlesLoaded) {
        if (usersLoaded && articlesLoaded) {
            isLoading = false
        }
    }

    // Fetch statistics from Firebase with real-time updates
    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()

            // Listen for users collection changes
            db.collection("users")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        NotificationManager.showError("Error listening for users: ${e.message}")
                        return@addSnapshotListener
                    }
                    totalUsers = snapshot?.size() ?: 0
                    usersLoaded = true
                }

            // Listen for articles collection changes
            db.collection("articles")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        NotificationManager.showError("Error listening for articles: ${e.message}")
                        return@addSnapshotListener
                    }

                    totalArticles = snapshot?.size() ?: 0

                    // Update category stats
                    val stats = mutableMapOf<String, Int>()
                    snapshot?.forEach { article ->
                        val category = article.getString("category") ?: "Uncategorized"
                        stats[category] = (stats[category] ?: 0) + 1
                    }
                    categoryStats = stats
                    articlesLoaded = true
                }

        } catch (e: Exception) {
            error = "Error setting up listeners: ${e.message}"
            NotificationManager.showError("Error setting up listeners: ${e.message}")
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackgroundTheme)
            .padding(16.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // Lottie Animation
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
                        modifier = Modifier.size(130.dp)
                    )
                    
                    // Loading text
                    val loadingFact = remember { RandomFact() }
                    Text(
                        text = loadingFact,
                        fontSize = 16.sp,
                        color = OnboardingTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error ?: "Unknown error",
                    color = Color.Red,
                    fontSize = 16.sp
                )
            }
        } else {
            // Main stats cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Total Users",
                    value = totalUsers.toString(),
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Articles",
                    value = totalArticles.toString(),
                    icon = Icons.Default.Article,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (categoryStats.isNotEmpty()) {
                // Category stats
                Text(
                    "Articles by Category",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(categoryStats.toList()) { (category, count) ->
                        StatCard(
                            title = category,
                            value = count.toString(),
                            icon = Icons.Default.Category
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No articles found",
                        color = OnboardingTextSecondary,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val borderColor = animateColorAsState(
        targetValue = OnboardingTextSecondary,
        animationSpec = tween(300)
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = borderColor.value,
                shape = RoundedCornerShape(4.dp)
            )
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(0.8f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Melrose,
                modifier = Modifier
                    .weight(0.2f)
                    .size(32.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
} 