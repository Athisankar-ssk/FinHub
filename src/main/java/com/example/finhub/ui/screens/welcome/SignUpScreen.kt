package com.example.finhub.ui.screens.welcome

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.finhub.ui.theme.AccentPurple
import com.example.finhub.ui.theme.BackgroundTheme
import com.example.finhub.ui.theme.Melrose
import com.example.finhub.ui.theme.OnboardingTextSecondary
import com.example.finhub.utils.getGoogleSignInRequest
import com.example.finhub.utils.handleGoogleSignInResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import com.example.finhub.data.database.FirebaseUserService
import com.example.finhub.ui.theme.Follow
import com.example.finhub.ui.theme.Red
import com.example.finhub.utils.NetworkUtils
import com.example.finhub.utils.NotificationManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = Firebase.auth
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Check if already logged in
    LaunchedEffect(Unit) {
        if (auth.currentUser != null) {
            navController.navigate("home") {
                popUpTo("signup") { inclusive = true }
            }
        }
    }

    val oneTapClient = remember { Identity.getSignInClient(context) }
    val signInRequest = remember { getGoogleSignInRequest() }

//    val googleSignInLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.StartIntentSenderForResult()
//    ) { result ->
//        isLoading = true
//        handleGoogleSignInResult(
//            result.data,
//            oneTapClient,
//            auth,
//            context
//        ) {
//            isLoading = false
//            scope.launch {
//                val interests = com.example.finhub.data.database.FirebaseUserPreferencesService.getUserInterests()
//                if (interests.isNullOrEmpty()) {
//                    navController.navigate("interest_selection") {
//                        popUpTo("signup") { inclusive = true }
//                    }
//                } else {
//                    navController.navigate("home") {
//                        popUpTo("signup") { inclusive = true }
//                    }
//                }
//            }
//        }
//    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                text = "Create Your Account",
                color = OnboardingTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use your email for registration",
                color = OnboardingTextSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = OnboardingTextSecondary) },
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

            // Email input
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

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = OnboardingTextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textStyle = TextStyle(color = Color.White),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = OnboardingTextSecondary
                        )
                    }
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Melrose,
                    unfocusedBorderColor = OnboardingTextSecondary,
                    cursorColor = Color.White
                )
            )

            // Sign Up Button
            Button(
                onClick = {
                    // First check for internet connectivity
                    if (!NetworkUtils.isInternetAvailable(context)) {
                        // Show no internet connection notification
                        NotificationManager.showError("No internet connection available")
                        return@Button
                    }
                    
                    if (validateInputs(name, email, password, context)) {
                        isLoading = true
                        error = null
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build()
                                    user?.updateProfile(profileUpdates)
                                        ?.addOnCompleteListener { profileTask ->
                                            scope.launch {
                                                try {
                                                    // Send email verification
                                                    user?.sendEmailVerification()
                                                        ?.addOnCompleteListener { emailVerificationTask ->
                                                            if (emailVerificationTask.isSuccessful) {
                                                                // Use coroutine scope to call suspend functions
                                                                scope.launch {
                                                                    try {
                                                                        // Store user data in Firestore
                                                                        FirebaseUserService.createOrUpdateUser(
                                                                            email = email,
                                                                            name = name,
                                                                            accountType = "email",
                                                                            emailVerified = false
                                                                        )

                                                                        // Show verification email sent notification
                                                                        NotificationManager.showSuccess("Account created! Please check your email to verify your account.")
                                                                        
                                                                        // Sign out the user after account creation
                                                                        // This forces them to sign in again after verifying email
                                                                        auth.signOut()
                                                                        
                                                                        // Instead of navigating to interest selection, navigate back to sign-in
                                                                        // with a message to check email for verification
                                                                        navController.navigate("signin") {
                                                                            popUpTo("signup") { inclusive = true }
                                                                        }
                                                                    } catch (e: Exception) {
                                                                        NotificationManager.showError("Error creating user profile: ${e.message}")
                                                                    }
                                                                }
                                                            } else {
                                                                // Failed to send verification email
                                                                val errorMessage = "Account created but failed to send verification email. You can request a new one later."
                                                                NotificationManager.showInfo(errorMessage)
                                                                
                                                                // Still navigate to interest selection but wrap in coroutine scope
                                                                scope.launch {
                                                                    try {
                                                                        // Store user data in Firestore
                                                                        FirebaseUserService.createOrUpdateUser(
                                                                            email = email,
                                                                            name = name,
                                                                            accountType = "email",
                                                                            emailVerified = false
                                                                        )
                                                                        // Sign out the user after account creation
                                                                        // This forces them to sign in again after verifying email
                                                                        auth.signOut()
                                                                        
                                                                        // Instead of navigating to interest selection, navigate back to sign-in
                                                                        // with a message to check email for verification
                                                                        navController.navigate("signin") {
                                                                            popUpTo("signup") { inclusive = true }
                                                                        }
                                                                    } catch (e: Exception) {
                                                                        NotificationManager.showError("Error creating user profile: ${e.message}")
                                                                    }
                                                                }
                                                            }
                                                        }
                                                } catch (e: Exception) {
                                                    val errorMessage = "Something went wrong during sign up. Please try again."
                                                    error = errorMessage
                                                    NotificationManager.showError(errorMessage)
                                                }
                                            }
                                            isLoading = false
                                        }
                                } else {
                                    // Get the Firebase error message
                                    val firebaseError = task.exception?.message ?: ""
                                    
                                    // Provide a more user-friendly message based on the error
                                    val errorMessage = when {
                                        firebaseError.contains("email address is already in use") -> 
                                            "This email is already registered. Please sign in instead."
                                            
                                        firebaseError.contains("password is invalid") -> 
                                            "Please choose a stronger password."
                                            
                                        firebaseError.contains("network") -> 
                                            "Network error. Please check your connection and try again."
                                                
                                        else -> "Sign up failed. Please try again."
                                    }
                                    
                                    error = errorMessage
                                    NotificationManager.showError(errorMessage)
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
                if (isLoading) {
                    // Show loading spinner
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 5.dp
                    )
                } else {
                    // Show regular text
                    Text("Create Account", fontWeight = FontWeight.SemiBold)
                }
            }

            error?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Already have an account? Sign In link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account ?", color = OnboardingTextSecondary)
                TextButton(onClick = { navController.navigate("signin") }) {
                    Text("Sign In", color = Melrose, fontWeight = FontWeight.Bold)
                }
            }

            // Spacer
            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign In Button
//            Button(
//                onClick = {
//                    oneTapClient.beginSignIn(signInRequest)
//                        .addOnSuccessListener { result ->
//                            googleSignInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent).build())
//                        }
//                        .addOnFailureListener { e ->
//                            Toast.makeText(context, "Google Sign-In Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
//                        }
//                },
//                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
//                shape = RoundedCornerShape(12.dp),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(50.dp)
//            ) {
//                Image(
//                    painter = painterResource(R.drawable.google_logo),
//                    contentDescription = null,
//                    modifier = Modifier.size(32.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text("Sign Up with Google", color = Color.Black, fontWeight = FontWeight.Bold)
//            }
        }
    }
}

// Helper Function
private fun validateInputs(name: String, email: String, password: String, context: android.content.Context): Boolean {
    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
        NotificationManager.showError("Please fill all fields")
        return false
    }
    
    // More comprehensive email validation
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    if (!email.matches(emailRegex.toRegex())) {
        NotificationManager.showError("Please enter a valid email address")
        return false
    }
    
    // Strong password validation using regex
    val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    
    if (!password.matches(passwordRegex.toRegex())) {
        // Provide specific feedback based on what's missing
        when {
            password.length < 8 -> {
                NotificationManager.showError("Password must be at least 8 characters long")
            }
            !password.any { it.isDigit() } -> {
                NotificationManager.showError("Password must contain at least one number")
            }
            !password.any { it.isLowerCase() } -> {
                NotificationManager.showError("Password must contain at least one lowercase letter")
            }
            !password.any { it.isUpperCase() } -> {
                NotificationManager.showError("Password must contain at least one uppercase letter")
            }
            !password.any { "@#$%^&+=!".contains(it) } -> {
                NotificationManager.showError("Password must contain at least one special character (@, #, $, etc.)")
            }
            password.contains(" ") -> {
                NotificationManager.showError("Password cannot contain spaces")
            }
            else -> {
                NotificationManager.showError("Password must be at least 8 characters with uppercase, lowercase, number, and special character")
            }
        }
        return false
    }
    
    // Name validation - prevent very short names
    if (name.length < 2) {
        NotificationManager.showError("Name is too short")
        return false
    }
    
    return true
}
