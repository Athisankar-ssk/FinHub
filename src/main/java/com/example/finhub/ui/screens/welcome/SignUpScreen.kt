package com.example.finhub.ui.screens.welcome

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
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
import kotlinx.coroutines.launch
import com.example.finhub.data.database.FirebaseUserService
import com.example.finhub.ui.theme.Follow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Melrose,
                    unfocusedBorderColor = OnboardingTextSecondary,
                    cursorColor = Color.White
                )
            )

            // Sign Up Button
            Button(
                onClick = {
                    if (validateInputs(name, email, password, context)) {
                        isLoading = true
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
                                                    // Store user data in Firestore
                                                    FirebaseUserService.createOrUpdateUser(
                                                        email = email,
                                                        name = name,
                                                        accountType = "email"
                                                    )

                                                    // Set logged in state
                                                    sharedPreferences.edit()
                                                        .putBoolean("is_logged_in", true)
                                                        .apply()

                                                    Toast.makeText(context, "Account Created Successfully", Toast.LENGTH_SHORT).show()
                                                    
                                                    // Navigate to interest selection for new users
                                                    navController.navigate("interest_selection") {
                                                        popUpTo("signup") { inclusive = true }
                                                    }
                                                } catch (e: Exception) {
                                                    error = "Error creating user: ${e.message}"
                                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            isLoading = false
                                        }
                                } else {
                                    error = "Sign up failed: ${task.exception?.message}"
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
                Text("Create Account", fontWeight = FontWeight.SemiBold)
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
fun validateInputs(name: String, email: String, password: String, context: android.content.Context): Boolean {
    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
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
