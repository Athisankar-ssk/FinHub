package com.example.finhub.ui.screens.welcome

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
import com.example.finhub.utils.getGoogleSignInRequest
import com.example.finhub.utils.handleGoogleSignInResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()

    val oneTapClient = remember { Identity.getSignInClient(context) }
    val signInRequest = remember { getGoogleSignInRequest() }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isLoading = true
        handleGoogleSignInResult(
            result.data,
            oneTapClient,
            auth,
            context
        ) {
            isLoading = false
            scope.launch {
                val interests = com.example.finhub.data.database.FirebaseUserPreferencesService.getUserInterests()
                if (interests.isNullOrEmpty()) {
                    navController.navigate("interest_selection") {
                        popUpTo("signup") { inclusive = true }
                    }
                } else {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create Your Account",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textStyle = TextStyle(color = Color.White),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF2CDCBB),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.White
                )
            )

            // Email input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textStyle = TextStyle(color = Color.White),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF2CDCBB),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color.White
                )
            )

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textStyle = TextStyle(color = Color.White),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF2CDCBB),
                    unfocusedBorderColor = Color.Gray,
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
                                isLoading = false
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build()
                                    user?.updateProfile(profileUpdates)
                                        ?.addOnCompleteListener {
                                            Toast.makeText(context, "Account Created Successfully", Toast.LENGTH_SHORT).show()
                                            scope.launch {
                                                val interests = com.example.finhub.data.database.FirebaseUserPreferencesService.getUserInterests()
                                                if (interests.isNullOrEmpty()) {
                                                    navController.navigate("interest_selection") {
                                                        popUpTo("signup") { inclusive = true }
                                                    }
                                                } else {
                                                    navController.navigate("home") {
                                                        popUpTo("signup") { inclusive = true }
                                                    }
                                                }
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2CDCBB)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Sign Up", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            // Spacer
            Spacer(modifier = Modifier.height(8.dp))

            // Already have an account? Sign In link
            TextButton(onClick = { navController.navigate("signin") }) {
                Text("Already have an account? Sign In", color = Color(0xFF2CDCBB))
            }

            // Spacer
            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign In Button
            Button(
                onClick = {
                    oneTapClient.beginSignIn(signInRequest)
                        .addOnSuccessListener { result ->
                            googleSignInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent).build())
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Google Sign-In Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Up with Google", color = Color.Black, fontWeight = FontWeight.Bold)
            }
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
