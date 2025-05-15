package com.example.finhub.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finhub.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.navigation.NavController

import android.widget.Toast
import com.example.finhub.utils.NotificationManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.finhub.data.database.FirebaseAdminService
import com.example.finhub.ui.screens.home.DevBytesTheme
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    // Add notification host to display notifications
    NotificationManager.NotificationHost()
    
    // Initialize selected tab
    var selectedTab by remember { 
        val savedTab = sharedPreferences.getString("admin_selected_tab", null)
        mutableStateOf(
            when (savedTab) {
                "news" -> 3  // Index of "News" tab
                else -> 0    // Default to Dashboard
            }
        )
    }

    // Clear the saved tab preference after the initial composition
    LaunchedEffect(Unit) {
        sharedPreferences.edit().remove("admin_selected_tab").apply()
    }
    
    var showDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Dash Board", "Users", "Story", "News")

    // Function to handle logout
    fun handleLogout() {
        try {
            FirebaseAdminService.handleAdminLogout(context)
            navController.navigate("welcome") {
                popUpTo("admin") { inclusive = true }
            }
        } catch (e: Exception) {
            NotificationManager.showError("Error logging out: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SideBackground)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "FinHub Admin",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        ConfirmationDialog(
            isVisible = showDialog,
            title = "Are you sure you want to logout?",
            confirmButtonText = "Yes, logout",
            cancelButtonText = "Cancel",
            onConfirm = {
                handleLogout()
                showDialog = false },
            onCancel = { showDialog = false }
        )

        // Tab Row
//        TabRow(
//            selectedTabIndex = selectedTab,
//            containerColor = Color.Transparent,
//            contentColor = Color.White,
//            indicator = { tabPositions ->
//                TabRowDefaults.Indicator(
//                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
//                    height = 2.dp,
//                    color = AccentPurple
//                )
//            }
//        ) {
//            tabs.forEachIndexed { index, title ->
//                Tab(
//                    selected = selectedTab == index,
//                    onClick = { selectedTab = index },
//                    text = {
//                        Text(
//                            text = title,
//                            fontSize = 14.sp,
//                            fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal,
//                            color = if (selectedTab == index) Color.White else OnboardingTextSecondary
//                        )
//                    }
//
//                )
//            }
//        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.LightGray,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth(),
            divider = {},
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(
                            tabPositions[selectedTab]
                        )
                        .height(3.dp)
                        .background(MediumVilot)
                )
            }

        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = tab,
                            color = if (selectedTab == index) DevBytesTheme.textPrimary else DevBytesTheme.textSecondary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            maxLines = 1
                        )
                    }
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> AdminDashboard()
            1 -> AdminUsers()
            2 -> AdminStory()
            3 -> AdminNews()
        }
    }
} 