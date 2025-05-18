package com.example.finhub.ui.screens.welcome

import android.widget.Toast
import com.example.finhub.utils.NotificationManager
import com.example.finhub.utils.NetworkUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finhub.data.database.FirebaseUserPreferencesService
import com.example.finhub.data.database.FirebaseUserService
import com.example.finhub.data.model.INTEREST_CATEGORIES
import com.example.finhub.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestSelectionScreen(navController: NavController) {
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Add notification host to display notifications
    NotificationManager.NotificationHost()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundTheme)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Header Section
        Text(
            text = "Choose Your\nInterests",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 40.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Select at least 3 topics to personalize your feed",
            color = OnboardingTextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Interests Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(INTEREST_CATEGORIES) { interest ->
                val isSelected = selectedInterests.contains(interest)
                InterestItem(
                    interest = interest,
                    isSelected = isSelected,
                    onSelect = {
                        selectedInterests = if (isSelected) {
                            selectedInterests - interest
                        } else {
                            selectedInterests + interest
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Section with Counter and Continue Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Text(
//                text = "${selectedInterests.size} topics selected",
//                color = OnboardingTextSecondary,
//                fontSize = 16.sp,
//                fontWeight = FontWeight.Medium
//            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Check internet connection first
                    if (!NetworkUtils.isInternetAvailable(context)) {
                        NotificationManager.showError("No internet connection. Please check your connection and try again.")
                        return@Button
                    }
                    
                    isLoading = true
                    scope.launch {
                        try {
                            // Save interests to user document
                            FirebaseUserService.saveUserInterests(selectedInterests.toList())
                            navController.navigate("home") {
                                popUpTo("interest_selection") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            NotificationManager.showError("Error saving interests: ${e.message}")
                        }
                        isLoading = false
                    }
                },
                enabled = selectedInterests.size >= 3 && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Follow,
                    contentColor = Color.White,
                    disabledContainerColor = Followed
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "Continue",
                        fontSize = 18.sp,
                        fontFamily = FinHubFont,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun InterestItem(
    interest: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val backgroundColor = animateColorAsState(
        targetValue = if (isSelected) Follow else Color.Transparent,
        animationSpec = tween(300)
    )

    val borderColor = animateColorAsState(
        targetValue = if (isSelected) Follow else OnboardingTextSecondary,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor.value)
            .border(
                width = 1.dp,
                color = borderColor.value,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onSelect)
            .height(72.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = interest,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
} 