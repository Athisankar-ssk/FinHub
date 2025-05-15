package com.example.finhub.admin

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.example.finhub.utils.NotificationManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import com.example.finhub.data.database.NewsRecord
import com.example.finhub.data.database.NewsRecordService
import com.example.finhub.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private val newsRecordService = NewsRecordService() // Single instance for all composables

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNews() {
    val context = LocalContext.current
    
    // Add notification host to display notifications
    NotificationManager.NotificationHost()
    
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var currentRecordId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val workManager = WorkManager.getInstance(context)
    
    // Collect news records using remember for state persistence
    val records = remember { mutableStateOf<List<NewsRecord>>(emptyList()) }
    
    // Filtered records based on search query
    val filteredRecords = remember(searchQuery, records.value) {
        if (searchQuery.isBlank()) {
            records.value
        } else {
            records.value.filter { record ->
                record.status.contains(searchQuery, ignoreCase = true) ||
                record.startTime.toString().contains(searchQuery, ignoreCase = true) ||
                record.endTime?.toString()?.contains(searchQuery, ignoreCase = true) == true ||
                record.duration?.toString()?.contains(searchQuery, ignoreCase = true) == true ||
                record.date.toString().contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Use DisposableEffect to handle subscription lifecycle
    DisposableEffect(Unit) {
        var isSubscribed = true
        
        scope.launch {
            newsRecordService.getRecordsFlow()
                .catch { e -> 
                    if (isSubscribed) {
                        NotificationManager.showError("Error loading history: ${e.message}")
                    }
                }
                .collect { 
                    if (isSubscribed) {
                        records.value = it
                    }
                }
        }

        // Cleanup when the composable is disposed
        onDispose {
            isSubscribed = false
        }
    }

    // API Keys
    val finnhubApiKey = "cuslsapr01qnihs7d1lgcuslsapr01qnihs7d1m0"
    val newsApiKey = "cda48b3dfe61437492014dec4ef5a703"
    val GnewsApiKey = "d3c91fe2dce9b0b8f4fb7302cb4043b6"
    val SerpApiKey = "b6db71dcb10bc63b92dcd1d100d742a63c866b30837a3254d5a61ee84ae8a630"

    // Function to clear status message after a delay
    fun clearStatusAfterDelay() {
        scope.launch {
//            delay(2000) // 2 seconds delay
            statusMessage = null
        }
    }

    // Observe work status
    LaunchedEffect(Unit) {
        // Check for existing work
        val workInfo = workManager.getWorkInfosForUniqueWork("newsFetch").get()
        isLoading = workInfo.any { !it.state.isFinished }
        
        // If no work is running, ensure status is cleared
        if (!isLoading) {
            statusMessage = null
            currentRecordId = null
        }

        // Observe work status changes
        workManager.getWorkInfosForUniqueWorkLiveData("newsFetch")
            .observeForever { workInfoList ->
                workInfoList?.let { infos ->
                    val workInfo = infos.firstOrNull()
                    scope.launch {
                        when (workInfo?.state) {
                            WorkInfo.State.RUNNING -> {
                                isLoading = true
                                val progress = workInfo.progress.getInt("progress", 0)
                                statusMessage = "Processing... $progress%"
                                // Update record status with progress
                                currentRecordId?.let { recordId ->
                                    newsRecordService.updateStatus(recordId, "processing", progress)
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                isLoading = false
                                statusMessage = "News fetch completed successfully!"
                                // Update record completion
                                currentRecordId?.let { recordId ->
                                    newsRecordService.updateRecordEnd(recordId, "completed")
                                    currentRecordId = null
                                }
                                clearStatusAfterDelay()
                            }
                            WorkInfo.State.FAILED -> {
                                isLoading = false
                                statusMessage = "News fetch failed"
                                // Update record failure
                                currentRecordId?.let { recordId ->
                                    newsRecordService.updateRecordEnd(recordId, "failed")
                                    currentRecordId = null
                                }
                                clearStatusAfterDelay()
                            }
                            WorkInfo.State.CANCELLED -> {
                                isLoading = false
                                statusMessage = "News fetch cancelled"
                                // Update record cancellation
                                currentRecordId?.let { recordId ->
                                    newsRecordService.updateRecordEnd(recordId, "cancelled")
                                    currentRecordId = null
                                }
                                clearStatusAfterDelay()
                            }
                            else -> {
                                if (!isLoading && currentRecordId != null) {
                                    // If we have a record ID but we're not loading, something went wrong
                                    newsRecordService.updateRecordEnd(currentRecordId!!, "failed")
                                    currentRecordId = null
                                }
                                isLoading = false
                                statusMessage = null
                            }
                        }
                    }
                }
            }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun startNewsFetch() {
        scope.launch {
            try {
                // Create new record first
                currentRecordId = newsRecordService.createRecord()
                
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val inputData = workDataOf(
                    "finnhubApiKey" to finnhubApiKey,
                    "newsApiKey" to newsApiKey,
                    "GnewsApiKey" to GnewsApiKey,
                    "serpApiKey" to SerpApiKey
                )

                val workRequest = OneTimeWorkRequestBuilder<NewsWorker>()
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                workManager.enqueueUniqueWork(
                    "newsFetch",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

                isLoading = true
                statusMessage = "Starting news fetch..."
            } catch (e: Exception) {
                NotificationManager.showError("Error starting fetch: ${e.message}")
                currentRecordId = null
            }
        }
    }

    fun stopNewsFetch() {
        workManager.cancelUniqueWork("newsFetch")
        scope.launch {
            currentRecordId?.let { recordId ->
                newsRecordService.updateRecordEnd(recordId, "cancelled")
                currentRecordId = null
            }
        }
        NotificationManager.showInfo("Stopping news fetch...")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackgroundTheme)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fetch/Stop News Button
        Button(
            onClick = { showDialog = true },
            enabled = true,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLoading) Color.Red else Follow,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Stop Fetching",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Fetch News"
                    )
                    Text(
                        "Fetch Latest News",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        ConfirmationDialog(
            isVisible = showDialog,
            title = if(isLoading) "Are you sure you want to stop fetching the news ?" else "Are you sure you want to start fetching the news ?",
            confirmButtonText = if(isLoading) "Yes, stop" else "Yes, proceed",
            cancelButtonText = "Cancel",
            onConfirm = {
                showDialog = false
                if (isLoading) {
                    stopNewsFetch()
                } else {
                    if (!isNetworkAvailable(context)) {
                        NotificationManager.showError("No internet connection available")
                    }
                    startNewsFetch()
                }
            },
            onCancel = { showDialog = false }
        )

        // Status Message
        if (isLoading || statusMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardPurple
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = statusMessage ?: "Processing...",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // History Section
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Fetching History",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Search by date, status, time...", color = OnboardingTextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = OnboardingTextSecondary
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Follow,
                unfocusedBorderColor = OnboardingTextSecondary
            ),
            shape = RoundedCornerShape(4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRecords) { record ->
                NewsRecordCard(record = record)
            }
        }
    }
}

@Composable
fun NewsRecordCard(record: NewsRecord) {
    val dateFormat = SimpleDateFormat("MMM, dd yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val borderColor = animateColorAsState(
        targetValue = OnboardingTextSecondary,
        animationSpec = tween(300)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = borderColor.value,
                shape = RoundedCornerShape(4.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date (top left) and Status (top right) in the same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date (top left)
                Text(
                    text = "Date: " + dateFormat.format(record.date),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                // Status (top right)
                Text(
                    text = "${record.status.capitalize()}",
                    color = when (record.status) {
                        "completed" -> Green
                        "failed" -> Red
                        "cancelled" -> Yellow
                        else -> MediumVilot
                    },
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Started: ${timeFormat.format(record.startTime)}",
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // End Time (if available)
            record.endTime?.let { endTime ->
                Text(
                    text = "Ended: ${timeFormat.format(endTime)}",
                    color = Color.White,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Duration (if available)
            record.duration?.let { duration ->
                Text(
                    text = "Duration: ${formatDuration(duration)}",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, remainingSeconds)
        minutes > 0 -> String.format("%d:%02d", minutes, remainingSeconds)
        else -> String.format("%d seconds", remainingSeconds)
    }
}

private fun String.capitalize() = this.replaceFirstChar { 
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
}


