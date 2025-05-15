package com.example.finhub.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.finhub.ui.components.NotificationCard
import com.example.finhub.ui.theme.AccentPurple

/**
 * Manages in-app notifications/toasts
 */
object NotificationManager {
    // Notification state
    private val showNotification = mutableStateOf(false)
    private val notificationMessage = mutableStateOf("")
    private val notificationIcon = mutableStateOf<ImageVector?>(null)
    private val notificationBgColor = mutableStateOf(Color(0xFFF5F5F5))
    private val notificationTextColor = mutableStateOf(Color(0xFF333333))
    private val notificationAccentColor = mutableStateOf(Color(0xFF4CAF50))
    private val notificationDuration = mutableStateOf(2000L)
    
    /**
     * Show a success notification
     */
    fun showSuccess(message: String, durationMillis: Long = 2000) {
        notificationMessage.value = message
        notificationIcon.value = Icons.Filled.Check
        notificationBgColor.value = Color(0xFFE8F5E9)
        notificationTextColor.value = Color(0xFF2E7D32)
        notificationAccentColor.value = Color(0xFF4CAF50)
        notificationDuration.value = durationMillis
        showNotification.value = true
    }
    
    /**
     * Show an error notification
     */
    fun showError(message: String, durationMillis: Long = 2000) {
        notificationMessage.value = message
        notificationIcon.value = Icons.Filled.Error
        notificationBgColor.value = Color(0xFFFFEBEE)
        notificationTextColor.value = Color(0xFFC62828)
        notificationAccentColor.value = Color(0xFFF44336)
        notificationDuration.value = durationMillis
        showNotification.value = true
    }
    
    /**
     * Show an info notification
     */
    fun showInfo(message: String, durationMillis: Long = 2000) {
        notificationMessage.value = message
        notificationIcon.value = Icons.Filled.Info
        notificationBgColor.value = Color(0xFFE3F2FD)
        notificationTextColor.value = Color(0xFF1565C0)
        notificationAccentColor.value = Color(0xFF2196F3)
        notificationDuration.value = durationMillis
        showNotification.value = true
    }
    
    /**
     * Show a bookmark notification
     */
    fun showBookmarked(message: String, durationMillis: Long = 2000) {
        notificationMessage.value = message
        notificationIcon.value = Icons.Filled.BookmarkAdded
        notificationBgColor.value = Color(0xFFF3E5F5)
        notificationTextColor.value = Color(0xFF6A1B9A)
        notificationAccentColor.value = AccentPurple
        notificationDuration.value = durationMillis
        showNotification.value = true
    }

    fun showBookmarkRemoved(message: String, durationMillis: Long = 2000) {
        notificationMessage.value = message
        notificationIcon.value = Icons.Filled.BookmarkRemove
        notificationBgColor.value = Color(0xFFF3E5F5)
        notificationTextColor.value = Color(0xFF6A1B9A)
        notificationAccentColor.value = AccentPurple
        notificationDuration.value = durationMillis
        showNotification.value = true
    }
    
    /**
     * Show a custom notification
     */
    fun showCustom(
        message: String,
        icon: ImageVector? = null,
        backgroundColor: Color = Color(0xFFF5F5F5),
        textColor: Color = Color(0xFF333333),
        accentColor: Color = Color(0xFF4CAF50),
        durationMillis: Long = 2000
    ) {
        notificationMessage.value = message
        notificationIcon.value = icon
        notificationBgColor.value = backgroundColor
        notificationTextColor.value = textColor
        notificationAccentColor.value = accentColor
        notificationDuration.value = durationMillis
        showNotification.value = true
    }
    
    /**
     * Dismiss the current notification
     */
    fun dismiss() {
        showNotification.value = false
    }
    
    /**
     * Composable to render the notification
     * This should be placed in your main activity or a common parent composable
     */
    @Composable
    fun NotificationHost() {
        NotificationCard(
            message = notificationMessage.value,
            icon = notificationIcon.value,
            backgroundColor = notificationBgColor.value,
            textColor = notificationTextColor.value,
            accentColor = notificationAccentColor.value,
            durationMillis = notificationDuration.value,
            showNotification = showNotification.value,
            onDismiss = { showNotification.value = false }
        )
    }
}
