package com.example.finhub.data.database

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseUserPreferencesService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val preferencesCollection = db.collection("user_preferences")

    suspend fun saveUserInterests(interests: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        val data = mapOf("interests" to interests)
        preferencesCollection.document(uid).set(data).await()
    }

    suspend fun getUserInterests(): List<String> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val doc = preferencesCollection.document(uid).get().await()
        return doc.get("interests") as? List<String> ?: emptyList()
    }

    suspend fun updateUserInterests(interests: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        preferencesCollection.document(uid).update("interests", interests).await()
    }
} 