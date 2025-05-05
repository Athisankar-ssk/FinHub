package com.example.finhub.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finhub.data.database.FirebaseUserPreferencesService
import com.example.finhub.data.model.INTEREST_CATEGORIES
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

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
            followedTopics = FirebaseUserPreferencesService.getUserInterests()
        } catch (e: Exception) {
            errorMessage = "Failed to load followed topics."
        }
        isLoading = false
    }

    val suggestedTopics = INTEREST_CATEGORIES.filterNot { followedTopics.contains(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Personalize Feed",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else {
            errorMessage?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
            }
            // Combine followed and suggested topics for a single scrollable list
            val allTopics = followedTopics + suggestedTopics
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(allTopics) { topic ->
                    val isFollowed = followedTopics.contains(topic)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF232323)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topic,
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val newList = if (isFollowed) followedTopics - topic else followedTopics + topic
                                    followedTopics = newList
                                    scope.launch {
                                        FirebaseUserPreferencesService.saveUserInterests(newList)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = if (isFollowed)
                                    ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2350)) // Purple for Followed
                                else
                                    ButtonDefaults.buttonColors(containerColor = Color(0xFF6C47FF)), // Teal for Follow
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (isFollowed) "✔ Followed" else "✚ Follow",
                                    color = if (isFollowed) Color.White else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 