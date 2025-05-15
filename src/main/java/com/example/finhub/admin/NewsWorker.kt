package com.example.finhub.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.finhub.MainActivity
import com.example.finhub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class NewsWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val CHANNEL_ID = "news_fetch_channel"
    private val NOTIFICATION_ID = 1

    init {
        createNotificationChannel()
    }

    override suspend fun doWork(): Result {
        try {
            // Create notification for foreground service
            setForeground(createForegroundInfo("Starting news collection..."))
            
            val collector = NewsDataCollector(
                finnhubApiKey = inputData.getString("finnhubApiKey") ?: "",
                newsApiKey = inputData.getString("newsApiKey") ?: "",
                GnewsApiKey = inputData.getString("GnewsApiKey") ?: "",
                serpApiKey = inputData.getString("serpApiKey") ?: "",
                context = context
            )

            // Set progress callback
            collector.setProgressCallback { module, current, total, articleTitle ->
                // Check if work is cancelled
                if (isStopped) {
                    return@setProgressCallback
                }
                
                // Launch a coroutine for progress updates
                runBlocking {
                    val progress = (current.toFloat() / total * 100).toInt()
                    // Update progress data
                    setProgress(Data.Builder().putInt("progress", progress).build())
                    
                    // Create and set foreground info
                    val foregroundInfo = createForegroundInfo(
                        "$module: Processing article $current of $total",
                        progress,
                        total,
                        articleTitle
                    )
                    setForeground(foregroundInfo)
                }
            }

            // Check if already cancelled
            if (isStopped) {
                showCompletionNotification(false, "News fetch was cancelled")
                return Result.failure()
            }

            // Use coroutineScope to properly await the news collection
            collector.collectAndStoreNews() // Now properly awaited since collectAndStoreNews is suspend

            // Show completion notification only if not cancelled
            if (!isStopped) {
                showCompletionNotification(true, "Successfully processed all news articles!")
                return Result.success()
            } else {
                showCompletionNotification(false, "News fetch was cancelled")
                return Result.failure()
            }
            
        } catch (e: Exception) {
            Log.e("NewsWorker", "Error in worker: ${e.message}")
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "No internet connection. Please check your network settings."
                isStopped -> "News fetch was cancelled"
                else -> "Error: ${e.message}"
            }
            showCompletionNotification(false, errorMessage)
            return Result.failure()
        }
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openAdminNews", true)
            putExtra("adminTab", "news")  // Add specific tab information
        }
        
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "News Fetch Progress"
            val descriptionText = "Shows progress of news fetching and processing"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(
        message: String,
        progress: Int = 0,
        total: Int = 100,
        articleTitle: String? = null
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Fetching News")
            .setContentText(message)
            .apply {
                if (articleTitle != null) {
                    setSubText(articleTitle)
                }
            }
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setProgress(total, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createPendingIntent())
            .setAutoCancel(false)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(success: Boolean, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(if (success) "News Fetch Complete" else "News Fetch Failed")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createPendingIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
} 