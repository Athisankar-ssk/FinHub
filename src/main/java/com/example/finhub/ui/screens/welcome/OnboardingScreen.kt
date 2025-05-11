package com.example.finhub.ui.screens.welcome

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finhub.R
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.finhub.data.database.FirebaseUserPreferencesService
import com.example.finhub.ui.theme.*

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(navController: NavController, context: Context) {
    // Check if the user is logged in
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", true)

    // If either logged in or authenticated, navigate to home
    if (isLoggedIn || FirebaseAuth.getInstance().currentUser != null) {
        LaunchedEffect(Unit) {
            val interests = FirebaseUserPreferencesService.getUserInterests()
            if (interests.isNullOrEmpty()) {
                navController.navigate("interest_selection") {
                    popUpTo("onboarding") { inclusive = true }
                }
            } else {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        }
        return
    }


    
    val iconGradient = Brush.linearGradient(listOf(AccentPurple, ButtonGradientStart))

    // Onboarding pages data
    data class OnboardingPage(val icon: ImageVector, val title: String, val desc: String)
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.Newspaper,
            title = "Sharp Insights. Short Reads",
            desc = "Stay informed with concise and clear business news summaries. " +
                    "All the key insights, delivered in a format made for speed"
        ),
        OnboardingPage(
            icon = Icons.Filled.Palette,
            title = "Personalized Insights",
            desc = "Get news tailored to your interests, region, and market behavior. " +
                    "Your feed adapts to what matters most to you"
        ),
        OnboardingPage(
            icon = Icons.Filled.FormatQuote,
            title = "One Story, Every Day",
            desc = "Discover the journey of one inspiring business mind every day " +
                    "Be inspired by their path to success and the lessons they've learned"
        )
    )
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SideBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Card fills the space above the bottom card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SideBackground)
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(50))
                            .background(iconGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pages[pagerState.currentPage].icon,
                            contentDescription = "Onboarding Icon",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = pages[pagerState.currentPage].title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pages[pagerState.currentPage].desc,
                        color = Color(0xDDC4C6DD),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            // Bottom Card is flush with the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(OnboardCard)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pager for icon and content only
                    HorizontalPager(
                        count = pages.size,
                        state = pagerState,
                        userScrollEnabled = true,
                        modifier = Modifier.weight(1f, fill = false)
                    ) { page ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(iconGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = pages[page].icon,
                                    contentDescription = "Onboarding Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = pages[page].title,
                                color = CardPurple,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = pages[page].desc,
                                color =CardPurple,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Page indicators (fixed position, update with pagerState.currentPage)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(pages.size) { i ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (i == pagerState.currentPage) AccentPurple else Color(0x332D1B54))
                            )
                            if (i < pages.size - 1) Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    // Next/Get Started Button (fixed position, updates with pagerState.currentPage)
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                sharedPreferences.edit().putBoolean("showOnboarding", true).apply()
                                navController.navigate("welcome") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}