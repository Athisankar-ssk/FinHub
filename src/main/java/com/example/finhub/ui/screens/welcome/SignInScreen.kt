package com.example.finhub.ui.screens.welcome

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.finhub.utils.NetworkUtils
import com.example.finhub.utils.NotificationManager
import com.example.finhub.admin.ConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetEmailSending by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }

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
                // Check if email is verified
                val isEmailVerified = auth.currentUser?.isEmailVerified ?: false
                
                if (!isEmailVerified) {
                    // Email not verified, show verification dialog
                    showVerificationDialog = true
                    // Make sure logged_in is false
                    sharedPreferences.edit()
                        .putBoolean("is_logged_in", false)
                        .apply()
                } else {
                    // Email is verified, proceed with normal flow
                    // Update last login time for regular users
                    FirebaseUserService.updateLastLogin()
                    
                    // Check if user is logged in via SharedPreferences
                    val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
                    
                    if (isLoggedIn) {
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
            label = { Text("Email", color = OnboardingTextSecondary, fontFamily = FinHubFont) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = OnboardingTextSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),

            textStyle = TextStyle(color = Color.White, fontFamily = FinHubFont),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Melrose,
                unfocusedBorderColor = OnboardingTextSecondary,
                cursorColor = Color.White
            )

        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = OnboardingTextSecondary,  fontFamily = FinHubFont) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = OnboardingTextSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textStyle = TextStyle(color = Color.White),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val contentDescription = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = contentDescription,
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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // First check for internet connectivity
                if (!NetworkUtils.isInternetAvailable(context)) {
                    // Show no internet connection notification
                    NotificationManager.showError("No internet connection available")
                    return@Button
                }

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
                                                if (FirebaseAdminService.isAdminCredentials(email)) {
                                                    // Set admin session
                                                    FirebaseAdminService.setAdminSession(context)
                                                    // Navigate to admin screen
                                                    navController.navigate("admin") {
                                                        popUpTo("signin") { inclusive = true }
                                                    }
                                                } else {
                                                    // Update last login time
                                                    FirebaseUserService.updateLastLogin()
                                                    
                                                    // Force reload the user to get the latest verification status
                                                    auth.currentUser?.reload()?.addOnCompleteListener { reloadTask ->
                                                        // After reload, get the latest verification status
                                                        val isEmailVerified = auth.currentUser?.isEmailVerified ?: false
                                                        
                                                        // Only set logged in state if email is verified
                                                        if (isEmailVerified) {
                                                            sharedPreferences.edit()
                                                                .putBoolean("is_logged_in", true)
                                                                .apply()
                                                            
                                                            // Email is verified, proceed with normal flow
                                                            scope.launch {
                                                                try {
                                                                    // Update email verification status in Firestore
                                                                    FirebaseUserService.createOrUpdateUser(
                                                                        email = email,
                                                                        name = auth.currentUser?.displayName ?: "",
                                                                        accountType = "email",
                                                                        emailVerified = true
                                                                    )

                                                                    // Check for interests and navigate appropriately
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
                                                                } catch (e: Exception) {
                                                                    NotificationManager.showError("Error loading user data: ${e.message}")
                                                                }
                                                            }
                                                        } else {
                                                            // Make sure logged_in is false if email is not verified
                                                            sharedPreferences.edit()
                                                                .putBoolean("is_logged_in", false)
                                                                .apply()
                                                                
                                                            // Show verification required message
                                                            NotificationManager.showInfo("Please verify your email before continuing. Check your inbox or spam folder.")
                                                            
                                                            // Set dialog visibility to true
                                                            showVerificationDialog = true
                                                            
                                                            // Stay on sign-in screen
                                                            // Do not navigate further until email is verified
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Provide a more user-friendly message
                                                val errorMessage = "Something went wrong during sign in. Please try again."
                                                error = errorMessage
                                                NotificationManager.showError(errorMessage)
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    } else {
                                        // Get the Firebase error message
                                        val firebaseError = task.exception?.message ?: ""

                                        // Provide a more user-friendly message based on the error
                                        val errorMessage = when {
                                            firebaseError.contains("no user record") ||
                                                    firebaseError.contains("user may have been deleted") ->
                                                "No account found with this email. Please check your email or sign up."

                                            firebaseError.contains("password is invalid") ->
                                                "Invalid password. Please try again."

                                            firebaseError.contains("blocked") ->
                                                "Too many failed attempts. Please try again later."

                                            else -> "Invalid credentials. Please check your email and password."
                                        }

                                        error = errorMessage
                                        NotificationManager.showError(errorMessage)
                                        isLoading = false
                                    }
                                }
                        } catch (e: Exception) {
                            // Provide a more user-friendly message
                            val errorMessage = "Something went wrong during sign in. Please try again."
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
                Text("Continue", fontFamily = FinHubFont, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Forgot Password link
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                resetEmail = email // Pre-fill with current email if any
                showPasswordResetDialog = true
            }) {
                Text("Forgot Password ?",
                    color = Melrose,
                    fontSize = 14.sp,
                    fontFamily = FinHubFont,
                    fontWeight = FontWeight.Bold
                )
            }
        }


        error?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Sign up link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account ?", color = OnboardingTextSecondary)
            TextButton(onClick = { navController.navigate("signup") }) {
                Text("Sign Up", color = Melrose, fontFamily = FinHubFont, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Email Verification Dialog
    if (showVerificationDialog) {
        ModalBottomSheet(
            onDismissRequest = { showVerificationDialog = false },
            containerColor = BottomCard,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Email Verification Required",
                    color = CardPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    "Your email address needs to be verified before you can continue. " +
                    "Please check your inbox or spam folder for the verification email.",
                    fontSize = 14.sp,
                    color = CardPurple,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                var isVerificationEmailSending by remember { mutableStateOf(false) }
                
                Button(
                    onClick = {
                        if (!NetworkUtils.isInternetAvailable(context)) {
                            NotificationManager.showError("No internet connection available")
                            return@Button
                        }
                        
                        isVerificationEmailSending = true
                        auth.currentUser?.sendEmailVerification()
                            ?.addOnCompleteListener { task ->
                                isVerificationEmailSending = false
                                if (task.isSuccessful) {
                                    NotificationManager.showSuccess("Verification email sent. Please check your inbox.")
                                    showVerificationDialog = false
                                } else {
                                    NotificationManager.showError("Failed to send verification email. Please try again later.")
                                }
                            }
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    border = BorderStroke(1.dp, Follow),
                    colors = ButtonDefaults.buttonColors(containerColor = Follow)
                ) {
                    if (isVerificationEmailSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 5.dp
                        )
                    } else {
                        Text("Resend Verification Email")
                    }
                }
                
                TextButton(
                    onClick = {
                        auth.signOut()
                        showVerificationDialog = false
                    },
                    border = BorderStroke(1.dp, CardPurple),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = CardPurple)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Password Reset Dialog
    if (showPasswordResetDialog) {
        ModalBottomSheet(
            onDismissRequest = { showPasswordResetDialog = false },
            containerColor = BottomCard,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset Password",
                    color = CardPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    "Enter your email address and we'll send you a link to reset your password.",
                    fontSize = 14.sp,
                    color = CardPurple,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("Email", color = CardPurple,  fontFamily = FinHubFont) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Follow,
                        unfocusedBorderColor = CardPurple,
                        cursorColor = CardPurple
                    ),
                    textStyle = TextStyle(color = CardPurple)
                )

                if (resetEmailSending) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentPurple
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            // Validate email first
                            val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
                            if (resetEmail.isEmpty() || !resetEmail.matches(emailRegex.toRegex())) {
                                NotificationManager.showError("Please enter a valid email address")
                                return@Button
                            }

                            // Check for internet connectivity
                            if (!NetworkUtils.isInternetAvailable(context)) {
                                NotificationManager.showError("No internet connection available")
                                return@Button
                            }

                            resetEmailSending = true

                            // Send password reset email
                            auth.sendPasswordResetEmail(resetEmail)
                                .addOnCompleteListener { task ->
                                    resetEmailSending = false
                                    if (task.isSuccessful) {
                                        NotificationManager.showSuccess("Password reset email sent. Please check your inbox.")
                                        showPasswordResetDialog = false
                                    } else {
                                        val errorMessage = when {
                                            task.exception?.message?.contains("no user record") == true ->
                                                "No account found with this email."
                                            else -> "Failed to send reset email. Please try again."
                                        }
                                        NotificationManager.showError(errorMessage)
                                    }
                                }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Follow,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Follow),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !resetEmailSending
                    ) {
                        Text("Send Reset Link", color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedButton(
                        onClick = { showPasswordResetDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = CardPurple
                        ),
                        border = BorderStroke(1.dp, CardPurple),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !resetEmailSending
                    ) {
                        Text("Cancel", color = CardPurple)
                    }
                }
            }
        }
    }
}

// Add validation function
private fun validateInputs(email: String, password: String, context: Context): Boolean {
    if (email.isEmpty() || password.isEmpty()) {
        NotificationManager.showError("Please fill all fields")
        return false
    }

    // More comprehensive email validation
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    if (!email.matches(emailRegex.toRegex())) {
        NotificationManager.showError("Please enter a valid email address")
        return false
    }

    return true
}
