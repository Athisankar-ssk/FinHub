package com.example.finhub.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finhub.data.model.INTEREST_CATEGORIES
import com.example.finhub.data.database.FirebaseUserPreferencesService
import kotlinx.coroutines.launch

@Composable
fun InterestSelectionScreen(navController: NavController, onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    var selectedInterests by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch user interests on first composition
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            selectedInterests = FirebaseUserPreferencesService.getUserInterests()
        } catch (e: Exception) {
            errorMessage = "Failed to load interests."
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Your Interests",
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
            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                INTEREST_CATEGORIES.forEach { category ->
                    val isSelected = selectedInterests.contains(category)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                val newList = if (isSelected) {
                                    selectedInterests - category
                                } else {
                                    selectedInterests + category
                                }
                                selectedInterests = newList
                                // Update Firestore live
                                scope.launch {
                                    FirebaseUserPreferencesService.saveUserInterests(newList)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF2CDCBB) else Color(0xFF232323)
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
                                text = category,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onComplete() },
                enabled = selectedInterests.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2CDCBB))
            ) {
                Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
} 