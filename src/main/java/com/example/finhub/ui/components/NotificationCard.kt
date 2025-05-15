package com.example.finhub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay

/**
 * A customizable notification card that appears and disappears automatically
 * 
 * @param message The message to display in the notification
 * @param icon Optional icon to display next to the message
 * @param backgroundColor Background color of the notification card
 * @param textColor Text color for the notification message
 * @param durationMillis How long the notification should stay visible (in milliseconds)
 * @param showNotification Boolean state that controls whether the notification is visible
 * @param onDismiss Callback when notification is dismissed
 */
@Composable
fun NotificationCard(
    message: String,
    icon: ImageVector? = null,
    backgroundColor: Color = Color(0xFFF5F5F5),
    textColor: Color = Color(0xFF333333),
    accentColor: Color = Color(0xFF4CAF50),
    durationMillis: Long = 2000,
    showNotification: Boolean,
    onDismiss: () -> Unit
) {
    // Auto-dismiss after duration
    LaunchedEffect(showNotification) {
        if (showNotification) {
            delay(durationMillis)
            onDismiss()
        }
    }

    if (showNotification) {
        Popup(alignment = Alignment.BottomCenter,
            offset = IntOffset(0, -100)
        ) {
            AnimatedVisibility(
                visible = showNotification,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Left accent bar
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(48.dp)
                                .background(accentColor)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Icon if provided
                            icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            // Message text
                            Text(
                                text = message,
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to show a notification and automatically handle its state
 */
@Composable
fun ShowNotification(
    message: String,
    icon: ImageVector? = null,
    backgroundColor: Color = Color(0xFFF5F5F5),
    textColor: Color = Color(0xFF333333),
    accentColor: Color = Color(0xFF4CAF50),
    durationMillis: Long = 2000
) {
    var showNotification by remember { mutableStateOf(true) }
    
    NotificationCard(
        message = message,
        icon = icon,
        backgroundColor = backgroundColor,
        textColor = textColor,
        accentColor = accentColor,
        durationMillis = durationMillis,
        showNotification = showNotification,
        onDismiss = { showNotification = false }
    )
}
