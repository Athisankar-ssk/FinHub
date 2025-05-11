package com.example.finhub.data.database

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseAdminService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private const val ADMIN_COLLECTION = "admins"
    private const val ADMIN_EMAIL = "admin@finhub.com" // Replace with actual admin email
    private const val ADMIN_PASSWORD = "FinHub@2025" // Replace with actual admin password

    // Check if the provided credentials match admin credentials
    suspend fun isAdminCredentials(email: String, password: String): Boolean {
        return email == ADMIN_EMAIL && password == ADMIN_PASSWORD
    }

    // Set admin session in shared preferences
    fun setAdminSession(context: Context) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_admin", true)
            .apply()
    }

    // Clear admin session
    fun clearAdminSession(context: Context) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_admin", false)
            .apply()
    }

    // Check if current session is admin
    fun isAdminSession(context: Context): Boolean {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_admin", false)
    }

    // Handle admin logout
    fun handleAdminLogout(context: Context) {
        auth.signOut()
        clearAdminSession(context)
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", false)
            .apply()
    }
} 