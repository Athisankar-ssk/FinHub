package com.example.finhub.ui.screens.today

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.finhub.ui.theme.*
import com.example.finhub.viewmodel.TodayStoryViewModel
import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayStoryScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: TodayStoryViewModel = viewModel(factory = TodayStoryViewModel.provideFactory(context))
    val todayStory by viewModel.todayStory.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = HomeBackgroundTheme)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Today's Story",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = OnboardingTextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val story = todayStory) {
                    null -> {
                        // Show loading or no story state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = AccentPurple,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading today's story...",
                                    color = OnboardingTextSecondary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    else -> {
                        // Display the story
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Title
                            Text(
                                text = story.title.replace("*",""),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Person and Date
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Person
                                Text(
                                    text = "Story of ${story.person}",
                                    color = OnboardingTextSecondary,
                                    fontSize = 16.sp
                                )

                                // Date
                                val date = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(story.timestamp.seconds),
                                    ZoneId.systemDefault()
                                )
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                    color = OnboardingTextSecondary,
                                    fontSize = 14.sp
                                )
                            }

                            // Story content
                            Text(
                                text = story.story,
                                color = Color.White,
                                fontSize = 18.sp,
                                lineHeight = 28.sp
                            )
                        }
                    }
                }
            }
        }
    }
} 