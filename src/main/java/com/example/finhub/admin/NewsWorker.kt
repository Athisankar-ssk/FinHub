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
import androidx.work.workDataOf
import android.graphics.BitmapFactory
import com.example.finhub.MainActivity
import com.example.finhub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class NewsWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val PROGRESS_CHANNEL_ID = "news_fetch_progress_channel"
    private val COMPLETION_CHANNEL_ID = "news_fetch_completion_channel"
    private val PROGRESS_NOTIFICATION_ID = 1
    private val COMPLETION_NOTIFICATION_ID = 2

    init {
        createNotificationChannels()
    }

    override suspend fun doWork(): Result {
        return try {
            // Get the module tracker to determine which module to run
            val moduleTracker = ModuleTracker(applicationContext)
            val moduleToRun = moduleTracker.getNextModuleToRun()
            val moduleName = moduleTracker.getModuleName(moduleToRun)
            
            // Show initial progress notification
            runBlocking {
                setForeground(createProgressNotification("Starting module: $moduleName", 0, 1))
            }
            
            // Create collector with progress callback
            val collector = NewsDataCollector(
                finnhubApiKey = inputData.getString("finnhubApiKey") ?: "",
                newsApiKey = inputData.getString("newsApiKey") ?: "",
                GnewsApiKey = inputData.getString("GnewsApiKey") ?: "",
                serpApiKey = inputData.getString("serpApiKey") ?: "",
                context = context
            )
            
            collector.setProgressCallback { module, progress, total, articleTitle ->
                // Check if work is cancelled
                if (isStopped) {
                    return@setProgressCallback
                }
                
                // Calculate percentage for AdminNews screen
                val percentage = if (total > 0) (progress * 100) / total else 0
                
                // Set progress data for WorkInfo (used by AdminNews)
                runBlocking {
                    setProgress(workDataOf("progress" to percentage))
                }
                
                // Update progress notification
                runBlocking {
                    setForeground(createProgressNotification(
                        "$moduleName: Processing article $progress of $total",
                        progress,
                        total,
                        articleTitle
                    ))
                }
            }
            
            // Check if already cancelled
            if (isStopped) {
                showCompletionNotification(false, "$moduleName was cancelled")
                return Result.failure()
            }
            
            // Run just one module
            // Use withContext to explicitly define the coroutine context
            withContext(Dispatchers.IO) {
                collector.collectAndStoreNewsForModule(moduleToRun)
            }
            
            // Mark this module as completed
            moduleTracker.markModuleCompleted(moduleToRun)
            
            // Show success notification
            showCompletionNotification(true, "$moduleName completed successfully")
            Result.success()
        } catch (e: Exception) {
            // Get the module tracker to determine which module was running
            val moduleTracker = ModuleTracker(applicationContext)
            val moduleToRun = moduleTracker.getNextModuleToRun()
            val moduleName = moduleTracker.getModuleName(moduleToRun)
            
            Log.e("NewsWorker", "Error in module $moduleName: ${e.message}")
            
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "No internet connection. Please check your network settings."
                isStopped -> "$moduleName was cancelled"
                else -> "Error in $moduleName: ${e.message}"
            }
            
            showCompletionNotification(false, errorMessage)
            Result.failure()
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

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Progress channel (low priority)
            val progressChannel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "News Fetch Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of news fetching and processing"
            }
            
            // Completion channel (high priority)
            val completionChannel = NotificationChannel(
                COMPLETION_CHANNEL_ID,
                "News Fetch Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows final status of news fetching operations"
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completionChannel)
        }
    }

    private fun createProgressNotification(
        message: String,
        progress: Int = 0,
        total: Int = 100,
        articleTitle: String? = null
    ): ForegroundInfo {
        // Calculate percentage
        val percentage = if (total > 0) (progress * 100) / total else 0
        
        // Format message with percentage
        val messageWithPercentage = "$message ($percentage%)" 
        
        val notification = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setContentTitle("FinHub News Update")
            .setContentText(messageWithPercentage)
            .apply {
                if (articleTitle != null) {
                    setSubText(articleTitle)
                }
            }
            .setSmallIcon(R.drawable.notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification))
            .setOngoing(true)
            .setProgress(total, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createPendingIntent())
            .setAutoCancel(false)
            .build()

        return ForegroundInfo(PROGRESS_NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(success: Boolean, message: String) {
        val icon = if (success) R.drawable.notification else R.drawable.notification
        val title = if (success) "✅ News Update Complete" else "❌ News Update Failed"
        
        val notification = NotificationCompat.Builder(context, COMPLETION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification))
            .setOngoing(false)  // Not ongoing, but will persist
            .setAutoCancel(true)  // Auto-cancel when clicked
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVibrate(longArrayOf(0, 250, 250, 250))  // Add vibration
            .setContentIntent(createPendingIntent())
            // Ensure it doesn't time out
            .setTimeoutAfter(0)
            .build()

        // Cancel the progress notification first
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
        
        // Show the completion notification with a different ID
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }
}