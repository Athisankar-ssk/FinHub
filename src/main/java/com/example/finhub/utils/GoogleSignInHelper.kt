package com.example.finhub.utils

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.example.finhub.data.database.FirebaseUserService

// Create and return the Google Sign-In request
fun getGoogleSignInRequest(): BeginSignInRequest {
    return BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId("781904681760-pbaqkjp7ndcf9q7ru6ojsd89lb9m7et7.apps.googleusercontent.com")
                .setFilterByAuthorizedAccounts(false)
                .build()
        )
        .build()
}

// Function to trigger Google Sign-In flow (this avoids duplicating code in SignIn and SignUp)
fun triggerGoogleSignIn(
    context: Context,
    oneTapClient: SignInClient,
    launcher: ManagedActivityResultLauncher<IntentSenderRequest, androidx.activity.result.ActivityResult>
) {
    oneTapClient.beginSignIn(getGoogleSignInRequest())
        .addOnSuccessListener { result ->
            val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
            launcher.launch(intentSenderRequest)
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Google Sign-In Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

// Handle result from Google Sign-In flow and authenticate with Firebase
fun handleGoogleSignInResult(
    resultData: android.content.Intent?,
    oneTapClient: SignInClient,
    auth: FirebaseAuth,
    context: Context,
    navController: NavController,
    onSuccess: () -> Unit
) {
    try {
        val credential = oneTapClient.getSignInCredentialFromIntent(resultData)
        val idToken = credential.googleIdToken
        if (idToken != null) {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val scope = CoroutineScope(Dispatchers.Main)
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
                                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
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
                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Google Sign In Failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Error: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
