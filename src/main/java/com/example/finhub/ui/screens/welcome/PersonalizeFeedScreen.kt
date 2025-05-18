package com.example.finhub.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finhub.data.database.FirebaseUserService
import com.example.finhub.data.model.INTEREST_CATEGORIES
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.finhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizeFeedScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var followedTopics by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch followed topics on first composition
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            followedTopics = FirebaseUserService.getUserInterests()
        } catch (e: Exception) {
            errorMessage = "Failed to load followed topics."
        }
        isLoading = false
    }

    val suggestedTopics = INTEREST_CATEGORIES.filterNot { followedTopics.contains(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SideBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Back Button
        TopAppBar(
            title = {
                Text(
                    text = "Personalize Feed",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
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
            modifier = Modifier.zIndex(1f),
        )

        if (isLoading) {
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
                modifier = Modifier.size(150.dp)
            )
        } else {
            errorMessage?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
            }
            
            // Combine followed and suggested topics for a single scrollable list
            val allTopics = followedTopics + suggestedTopics
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)

            ) {
                items(allTopics) { topic ->
                    val isFollowed = followedTopics.contains(topic)
                    TopicItem(
                        topic = topic,
                        isFollowed = isFollowed,
                        currentFollowedTopics = followedTopics,
                        onFollowToggle = { newList ->
                            followedTopics = newList
                            scope.launch {
                                FirebaseUserService.saveUserInterests(newList)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicItem(
    topic: String,
    isFollowed: Boolean,
    currentFollowedTopics: List<String>,
    onFollowToggle: (List<String>) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = topic,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val newList = if (isFollowed) {
                        currentFollowedTopics.toMutableList().apply {
                            remove(topic)
                        }
                    } else {
                        currentFollowedTopics.toMutableList().apply {
                            add(topic)
                        }
                    }
                    onFollowToggle(newList)
                },
                shape = RoundedCornerShape(4.dp),
                colors = if (isFollowed)
                    ButtonDefaults.buttonColors(containerColor = Followed)
                else
                    ButtonDefaults.buttonColors(containerColor = Follow),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    if (isFollowed) "✔ Followed" else "✚ Follow",
                    color = Color.White,
                    fontFamily = FinHubFont
                )
            }
        }
    }
} 