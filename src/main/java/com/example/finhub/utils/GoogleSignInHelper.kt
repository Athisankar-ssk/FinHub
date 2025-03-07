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
                        Toast.makeText(context, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        Toast.makeText(context, "Google Sign-In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(context, "Google ID token is null", Toast.LENGTH_SHORT).show()
        }
    } catch (e: ApiException) {
        Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
