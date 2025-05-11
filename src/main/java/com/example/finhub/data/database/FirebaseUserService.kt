package com.example.finhub.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseUserService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun createOrUpdateUser(
        email: String,
        name: String,
        accountType: String
    ) {
        val user = auth.currentUser ?: return
        
        // Check if user exists
        val doc = usersCollection.document(user.uid).get().await()
        val isNewUser = !doc.exists()

        // Create base user data
        val userData = mutableMapOf(
            "email" to email,
            "name" to name,
            "accountType" to accountType,
            "lastLoginAt" to Timestamp.now()
        )

        // Add createdAt for new users
        if (isNewUser) {
            userData["createdAt"] = Timestamp.now()
        }

        // Update or create user document
        usersCollection.document(user.uid)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    suspend fun updateLastLogin() {
        val user = auth.currentUser ?: return
        usersCollection.document(user.uid)
            .update("lastLoginAt", Timestamp.now())
            .await()
    }

    fun getUserInterestsFlow(): Flow<List<String>> = callbackFlow {
        val user = auth.currentUser ?: run {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener = usersCollection.document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                val interests = snapshot?.get("interests") as? List<String> ?: emptyList()
                trySend(interests)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getUserInterests(): List<String> {
        val user = auth.currentUser ?: return emptyList()
        val doc = usersCollection.document(user.uid).get().await()
        return doc.get("interests") as? List<String> ?: emptyList()
    }

    suspend fun saveUserInterests(interests: List<String>) {
        val user = auth.currentUser ?: return
        usersCollection.document(user.uid)
            .update("interests", interests)
            .await()
    }

    suspend fun userExists(): Boolean {
        val user = auth.currentUser ?: return false
        val doc = usersCollection.document(user.uid).get().await()
        return doc.exists()
    }

    suspend fun getCurrentUser(): Map<String, Any>? {
        val user = auth.currentUser ?: return null
        val doc = usersCollection.document(user.uid).get().await()
        return if (doc.exists()) doc.data else null
    }
} 