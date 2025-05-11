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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.finhub.data.database.FirebaseAdminService
import com.example.finhub.ui.screens.home.DevBytesTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Dash Board", "Users", "Story", "News")
    val context = LocalContext.current

    // Function to handle logout
    fun handleLogout() {
        try {
            FirebaseAdminService.handleAdminLogout(context)
            navController.navigate("signin") {
                popUpTo("admin") { inclusive = true }
            }
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error logging out: ${e.message}", Toast.LENGTH_LONG).show()
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
                IconButton(onClick = { showLogoutDialog = true }) {
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

        if (showLogoutDialog) {
            ModalBottomSheet(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = BottomCard,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Are you sure you want to logout ?",
                        color = CardPurple,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {handleLogout()},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = CardPurple
                            ),
                            border = BorderStroke(1.dp, CardPurple),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Yes, logout", color = CardPurple)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Follow,
                                contentColor = OnboardingTextSecondary
                            ),
                            border = BorderStroke(1.dp, Follow),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = DevBytesTheme.textPrimary)
                        }
                    }
                }
            }
        }

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