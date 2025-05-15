package com.example.finhub.ui.screens.welcome

import android.content.Context
import android.widget.Toast
import com.example.finhub.utils.NotificationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.finhub.R
import com.example.finhub.ui.theme.*
import com.example.finhub.utils.handleGoogleSignInResult
import com.example.finhub.utils.triggerGoogleSignIn
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.example.finhub.data.database.FirebaseUserService
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(navController: NavController) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    // Add notification host to display notifications
    NotificationManager.NotificationHost()

    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)


    // One Tap Google Sign-In client
    val oneTapClient = remember { Identity.getSignInClient(context) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            handleGoogleSignInResult(
                resultData = result.data,
                oneTapClient = oneTapClient,
                auth = auth,
                context = context,
                navController = navController
            ) {
                // After successful Google authentication
                scope.launch {
                    try {
                        val user = auth.currentUser!!
                        // Create or update user in Firestore
                        FirebaseUserService.createOrUpdateUser(
                            email = user.email!!,
                            name = user.displayName ?: user.email!!.substringBefore("@"),
                            accountType = "google"
                        )
                        
                        // Set logged in state
                        sharedPreferences.edit()
                            .putBoolean("is_logged_in", true)
                            .apply()

                        // Check if user exists and has interests
                        val userExists = FirebaseUserService.userExists()
                        val interests = FirebaseUserService.getUserInterests()
                        
                        if (!userExists || interests.isEmpty()) {
                            // New user or no interests, go to interest selection
                            navController.navigate("interest_selection") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        } else {
                            // Existing user with interests, go to home
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    } catch (e: Exception) {
                        NotificationManager.showError("Error: ${e.message}")
                    }
                }
            }
        }
    }

    // Check if already logged in
    LaunchedEffect(Unit) {
        if (auth.currentUser != null) {
            // Update last login time
            FirebaseUserService.updateLastLogin()
            // Navigate based on interests
            val interests = FirebaseUserService.getUserInterests()
            if (interests.isEmpty()) {
                navController.navigate("interest_selection") {
                    popUpTo("welcome") { inclusive = true }
                }
            } else {
                navController.navigate("home") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        }
    }

    val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", true)

    // If logged in, navigate to home
    if (isLoggedIn || auth.currentUser != null) {
        LaunchedEffect(Unit) {
            val interests = FirebaseUserService.getUserInterests()
            if (interests.isEmpty()) {
                navController.navigate("interest_selection") {
                    popUpTo("welcome") { inclusive = true }
                }
            } else {
                navController.navigate("home") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        }
        return
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundTheme)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            // Logo and App Name
            Spacer(modifier = Modifier.height(125.dp))
            Text(
                text = "FINHUB",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Headline and subtitle
            Text(
                text = "BUSINESS &",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "FINANCE NEWS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Finance, Business Simplified.",
                color = OnboardingTextSecondary,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Divider(
                color = OnboardingTextSecondary,
                thickness = 2.dp,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(vertical = 12.dp)
            )
            Spacer(modifier = Modifier.height(50.dp))
            // Google Sign-In Button
            Button(
                onClick = {
                    // Check for internet connectivity first
                    if (com.example.finhub.utils.NetworkUtils.isInternetAvailable(context)) {
                        triggerGoogleSignIn(
                            context = context,
                            oneTapClient = oneTapClient,
                            launcher = googleSignInLauncher
                        )
                    } else {
                        // Show no internet connection notification
                        com.example.finhub.utils.NotificationManager.showError("No internet connection available")
                    }
                },
                shape = RoundedCornerShape(4.dp),
                border = ButtonDefaults.outlinedButtonBorder,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Continue with Google",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Sign up with other email
            Button(
                onClick = { 
                    // Check for internet connectivity first
                    if (com.example.finhub.utils.NetworkUtils.isInternetAvailable(context)) {
                        navController.navigate("signup")
                    } else {
                        // Show no internet connection notification
                        com.example.finhub.utils.NotificationManager.showError("No internet connection available")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    contentColor = Color.White
                )
            ) {
                Text("Continue with Email", fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Sign in link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account ? ", color = OnboardingTextSecondary)
                TextButton(onClick = { navController.navigate("signin") }) {
                    Text("Sign In", color = Melrose, fontWeight = FontWeight.Bold)
                }
            }
        }
        // T&C and Privacy Policy at the bottom
//        Column(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(bottom = 16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = "By proceeding, I agree to the ",
//                color = OnboardingTextSecondary,
//                fontSize = 12.sp
//            )
//            Row {
//                TextButton(onClick = { /* TODO: T&C */ }) {
//                    Text("T&C", color = AccentPurple, fontSize = 12.sp)
//                }
//                Text(" and ", color = OnboardingTextSecondary, fontSize = 12.sp)
//                TextButton(onClick = { /* TODO: Privacy Policy */ }) {
//                    Text("Privacy Policy", color = AccentPurple, fontSize = 12.sp)
//                }
//            }
//        }
    }
}
