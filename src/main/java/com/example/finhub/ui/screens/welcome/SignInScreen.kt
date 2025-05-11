package com.example.finhub.ui.screens.welcome

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.finhub.utils.handleGoogleSignInResult
import com.example.finhub.utils.triggerGoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.identity.Identity
import android.content.Context
import kotlinx.coroutines.launch
import com.example.finhub.ui.theme.*
import com.example.finhub.data.database.FirebaseUserService
import com.example.finhub.data.database.FirebaseAdminService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Check if already logged in
    LaunchedEffect(Unit) {
        if (auth.currentUser != null) {
            // Check if it's an admin session
            if (FirebaseAdminService.isAdminSession(context)) {
                navController.navigate("admin") {
                    popUpTo("signin") { inclusive = true }
                }
            } else {
                // Update last login time for regular users
                FirebaseUserService.updateLastLogin()
                // Navigate based on interests
                val interests = FirebaseUserService.getUserInterests()
                if (interests.isEmpty()) {
                    navController.navigate("interest_selection") {
                        popUpTo("signin") { inclusive = true }
                    }
                } else {
                    navController.navigate("home") {
                        popUpTo("signin") { inclusive = true }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundTheme)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FINHUB",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Sign in to Continue",
            color = OnboardingTextSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = OnboardingTextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textStyle = TextStyle(color = Color.White),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Melrose,
                unfocusedBorderColor = OnboardingTextSecondary,
                cursorColor = Color.White
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = OnboardingTextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textStyle = TextStyle(color = Color.White),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Melrose,
                unfocusedBorderColor = OnboardingTextSecondary,
                cursorColor = Color.White
            )
        )

        Button(
            onClick = {
                if (validateInputs(email, password, context)) {
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            // First authenticate with Firebase
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        scope.launch {
                                            try {
                                                // After successful Firebase auth, check if it's admin credentials
                                                if (FirebaseAdminService.isAdminCredentials(email, password)) {
                                                    // Set admin session
                                                    FirebaseAdminService.setAdminSession(context)
                                                    // Navigate to admin screen
                                                    navController.navigate("admin") {
                                                        popUpTo("signin") { inclusive = true }
                                                    }
                                                } else {
                                                    // Regular user flow
                                                    // Update last login time
                                                    FirebaseUserService.updateLastLogin()

                                                    // Set logged in state
                                                    sharedPreferences.edit()
                                                        .putBoolean("is_logged_in", true)
                                                        .apply()

                                                    // Check for interests
                                                    val interests = FirebaseUserService.getUserInterests()
                                                    if (interests.isEmpty()) {
                                                        navController.navigate("interest_selection") {
                                                            popUpTo("signin") { inclusive = true }
                                                        }
                                                    } else {
                                                        navController.navigate("home") {
                                                            popUpTo("signin") { inclusive = true }
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                error = "Error signing in: ${e.message}"
                                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    } else {
                                        error = "Sign in failed: ${task.exception?.message}"
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                    }
                                }
                        } catch (e: Exception) {
                            error = "Error signing in: ${e.message}"
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            isLoading = false
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPurple,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold)
        }

        error?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Sign up link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account ?", color = OnboardingTextSecondary)
            TextButton(onClick = { navController.navigate("signup") }) {
                Text("Sign Up", color = Melrose, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Add validation function
private fun validateInputs(email: String, password: String, context: Context): Boolean {
    if (email.isEmpty() || password.isEmpty()) {
        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        return false
    }
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
        return false
    }
    if (password.length < 6) {
        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
        return false
    }
    return true
}
