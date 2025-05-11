package com.example.finhub.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finhub.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class UserData(
    val uid: String,
    val email: String?,
    val name: String?,
    val createdAt: Timestamp?,
    val lastLogin: Timestamp?,
    val interests: List<String> = emptyList(),
    val accountType: String = "email"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsers() {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var filteredUsers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var totalUsers by remember { mutableStateOf(0) }
    var activeUsers by remember { mutableStateOf(0) }
    var googleUsers by remember { mutableStateOf(0) }
    var emailUsers by remember { mutableStateOf(0) }

    // Fetch users from Firebase with real-time updates
    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        error = e.message
                        isLoading = false
                        return@addSnapshotListener
                    }

                    val usersList = snapshot?.documents?.map { doc ->
                        UserData(
                            uid = doc.id,
                            email = doc.getString("email"),
                            name = doc.getString("name"),
                            createdAt = doc.getTimestamp("createdAt"),
                            lastLogin = doc.getTimestamp("lastLoginAt"),
                            interests = doc.get("interests") as? List<String> ?: emptyList(),
                            accountType = doc.getString("accountType") ?: "email"
                        )
                    } ?: emptyList()

                    users = usersList
                    filteredUsers = usersList
                    totalUsers = usersList.size
                    
                    // Calculate statistics
                    val thirtyDaysAgo = Timestamp(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
                    activeUsers = usersList.count { it.lastLogin?.compareTo(thirtyDaysAgo) ?: 0 > 0 }
                    googleUsers = usersList.count { it.accountType == "google" }
                    emailUsers = usersList.count { it.accountType == "email" }
                    
                    isLoading = false
                }
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    // Filter users based on search query
    LaunchedEffect(searchQuery, users) {
        filteredUsers = if (searchQuery.isBlank()) {
            users
        } else {
            users.filter { user ->
                user.email?.contains(searchQuery, ignoreCase = true) == true ||
                user.name?.contains(searchQuery, ignoreCase = true) == true ||
                user.interests.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackgroundTheme)
            .padding(16.dp)
    ) {
        // Statistics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Total Users",
                value = totalUsers.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Active Users",
                value = activeUsers.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Google Users",
                value = googleUsers.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Email Users",
                value = emailUsers.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Search users...", color = OnboardingTextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = OnboardingTextSecondary
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Follow,
                unfocusedBorderColor = OnboardingTextSecondary
            ),
            shape = RoundedCornerShape(4.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Follow)
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
            // Users list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredUsers) { user ->
                    UserCard(user)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
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
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UserCard(user: UserData) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Name and Account Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.name ?: "Anonymous",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user.accountType.capitalize(),
                    color = Melrose,
                    fontSize = 14.sp
                )
            }
            
            // Email
            user.email?.let { email ->
                Text(
                    text = email,
                    color = OnboardingTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Dates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                user.createdAt?.let { date ->
                    Text(
                        text = "Joined: ${dateFormat.format(date.toDate())}",
                        color = OnboardingTextSecondary,
                        fontSize = 12.sp
                    )
                }
                user.lastLogin?.let { date ->
                    Text(
                        text = "Last login: ${dateFormat.format(date.toDate())}",
                        color = OnboardingTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Interests
            if (user.interests.isNotEmpty()) {
                Text(
                    text = "Interests: ${user.interests.joinToString(", ")}",
                    color = OnboardingTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
} 