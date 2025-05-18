package com.example.finhub.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finhub.MainActivity
import com.example.finhub.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class Story(
    @DocumentId
    val id: String = "",
    val person: String = "",
    val title: String = "",
    val story: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val lastUpdateDate: String = ""  // YYYY-MM-DD format
)

@RequiresApi(Build.VERSION_CODES.O)
class TodayStoryViewModel(private val context: Context) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val todayStoryCollection = db.collection("todaystory")
    private val storiesCollection = db.collection("stories")

    private val _todayStory = MutableStateFlow<Story?>(null)
    val todayStory: StateFlow<Story?> = _todayStory.asStateFlow()

    init {
        loadTodayStory()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTodayStory() {
        viewModelScope.launch {
            try {
                // Get today's story from todaystory collection
                val todayStoryDoc = todayStoryCollection.document("current_story").get().await()
                val today = getTodayDate()

                if (todayStoryDoc.exists()) {
                    val story = todayStoryDoc.toObject(Story::class.java)
                    if (story != null) {
                        // If story exists and was updated today, use it
                        if (story.lastUpdateDate == today) {
                            _todayStory.value = story
                        } else if (isAfter6AM()) {
                            // If story is from previous day and it's after 6 AM, fetch new story
                            fetchAndStoreNewStory()
                        } else {
                            // If before 6 AM, still show previous day's story
                            _todayStory.value = story
                        }
                    }
                } else if (isAfter6AM()) {
                    // If no story exists and it's after 6 AM, fetch new story
                    fetchAndStoreNewStory()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchAndStoreNewStory() {
        try {
            val today = getTodayDate()
            
            // First try to get story with today's timestamp
            val todayStories = storiesCollection
                .whereGreaterThanOrEqualTo("timestamp", Timestamp.now())
                .whereLessThanOrEqualTo("timestamp", Timestamp.now())
                .get()
                .await()
                .toObjects(Story::class.java)

            val selectedStory = if (todayStories.isNotEmpty()) {
                todayStories.first()
            } else {
                // If no today's story, get random story
                val allStories = storiesCollection
                    .get()
                    .await()
                    .toObjects(Story::class.java)
                
                if (allStories.isNotEmpty()) {
                    allStories.random()
                } else {
                    null
                }
            }

            selectedStory?.let { story ->
                // Create new story object with lastUpdateDate
                val todayStory = Story(
                    id = story.id,
                    person = story.person,
                    title = story.title,
                    story = story.story,
                    timestamp = story.timestamp,
                    lastUpdateDate = today
                )

                // Store in todaystory collection
                todayStoryCollection.document("current_story")
                    .set(todayStory)
                    .await()

                // Delete from stories collection
                storiesCollection.document(story.id)
                    .delete()
                    .await()

                // Update the state
                _todayStory.value = todayStory
                
                // Check if notification has been sent today
                val notificationDoc = todayStoryCollection.document("notification_status").get().await()
                val notificationSent = if (notificationDoc.exists()) {
                    notificationDoc.getBoolean("sent_today") == true && 
                    notificationDoc.getString("date") == today
                } else {
                    false
                }
                
                // If notification hasn't been sent today, send it and update status
                if (!notificationSent) {
                    // Send notification
                    sendTodayStoryNotification(todayStory)
                    
                    // Update notification status in Firestore
                    todayStoryCollection.document("notification_status")
                        .set(mapOf(
                            "sent_today" to true,
                            "date" to today,
                            "time" to Timestamp.now()
                        ))
                        .await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun sendTodayStoryNotification(story: Story) {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Today's Story",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for daily story updates"
            }
            
            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create an intent that opens the Today Story screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openTodayStory", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification) // Use your app's notification icon
            .setContentTitle("Today's Story Updated")
            .setContentText(story.title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Show the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    companion object {
        private const val CHANNEL_ID = "today_story_channel"
        private const val NOTIFICATION_ID = 1001
        
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TodayStoryViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return TodayStoryViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTodayDate(): String {
        return LocalDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isAfter6AM(): Boolean {
        val now = LocalDateTime.now(ZoneOffset.of("+05:30")) // IST
        return now.hour >= 6
    }
} 