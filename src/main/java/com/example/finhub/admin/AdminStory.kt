package com.example.finhub.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finhub.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Schema for Story
data class Story(
    val person: String = "",
    val title: String = "",
    val story: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

// Class to handle Gemini API requests
class GeminiService {
    private val geminiApiUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-001:generateContent?key=AIzaSyC53aFcBdKUmDJWdRnryl4xakEyYniCKeg"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Rate limiter variables
    private val maxRequestsPerMinute = 10
    private val requestTimestamps = mutableListOf<Long>()
    private val minuteInMillis = 60_000L

    // Ensure only 10 requests per minute
    private suspend fun waitForRateLimit() {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            // Remove timestamps older than a minute
            requestTimestamps.removeAll { it <= currentTime - minuteInMillis }

            // If we've made 10 requests in the last minute, wait until the oldest request is a minute old
            if (requestTimestamps.size >= maxRequestsPerMinute) {
                val oldestTimestamp = requestTimestamps.first()
                val waitTime = (oldestTimestamp + minuteInMillis) - currentTime
                if (waitTime > 0) {
                    delay(waitTime)
                }
                // Clear old timestamps after waiting
                requestTimestamps.removeAll { it <= System.currentTimeMillis() - minuteInMillis }
            }
        }
    }

    // Function to generate story using Gemini AI
    suspend fun makeGeminiApiRequest(prompt: String, retries: Int = 3): String? {
        return withContext(Dispatchers.IO) {
            var attempt = 0
            while (attempt < retries) {
                // Wait for rate limit before making request
                waitForRateLimit()

                try {
                    println("Sending Gemini API request (attempt ${attempt + 1}): $prompt")

                    // Escape special characters in the prompt
                    val escapedPrompt = prompt
                        .replace("\\", "\\\\")  // Escape backslashes first
                        .replace("\"", "\\\"")  // Escape quotes
                        .replace("\n", "\\n")   // Escape newlines
                        .replace("\r", "\\r")   // Escape carriage returns
                        .replace("\t", "\\t")   // Escape tabs

                    val jsonBody = """
                        {
                            "contents": [
                                {
                                    "parts": [
                                        {
                                            "text": "$escapedPrompt"
                                        }
                                    ]
                                }
                            ]
                        }
                    """.trimIndent()

                    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(geminiApiUrl)
                        .post(requestBody)
                        .build()

                    // Record request timestamp
                    requestTimestamps.add(System.currentTimeMillis())

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val json = JSONObject(responseBody)
                        val result = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        delay(10_000L)
                        return@withContext result
                    } else {
                        val errorBody = response.body?.string() ?: "No error details"
                        println("Gemini API request failed: ${response.code} - $errorBody")
                        if (response.code == 429) {
                            // Rate limit hit, wait 10 seconds before retry
                            delay(10_000L)
                            attempt++
                            continue
                        }
                        // Other errors, wait 10 seconds before retry
                        delay(10_000L)
                        attempt++
                    }
                } catch (e: Exception) {
                    println("Error in Gemini API request: ${e.javaClass.simpleName} - ${e.message}")
                    // Wait 10 seconds before retry
                    delay(10_000L)
                    attempt++
                }
            }
            println("All retry attempts failed for prompt: $prompt")
            delay(10_000L)
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStory() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val geminiService = remember { GeminiService() }

    // State variables
    var totalStories by remember { mutableStateOf(0) }
    var personInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPersonList by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var persons by remember { mutableStateOf<List<String>>(emptyList()) }
    var filteredPersons by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isGeneratingTitle by remember { mutableStateOf(false) }

    // Listen for total stories count and persons list
    LaunchedEffect(Unit) {
        // Listen for total stories
        db.collection("stories")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    error = "Error listening for stories: ${e.message}"
                    return@addSnapshotListener
                }
                totalStories = snapshot?.size() ?: 0

                // Update persons list whenever stories change
                persons = snapshot?.documents
                    ?.mapNotNull { it.getString("person") }
                    ?.distinct()
                    ?.sorted() ?: emptyList()
            }
    }

    // Update filtered persons when search query changes
    LaunchedEffect(searchQuery, persons) {
        filteredPersons = if (searchQuery.isBlank()) {
            persons
        } else {
            persons.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Function to add new story
    suspend fun addStory(personName: String) {
        try {
            isLoading = true
            // First, generate the story
            val storyPrompt =
                "Write a detailed story about the business personality $personName. " +
                        "Focus on their journey, achievements, and impact on business. " +
                        "Make it engaging and informative. Provide only the story text without any introduction, headings, or markdown formatting."

            val story = geminiService.makeGeminiApiRequest(storyPrompt)
            if (story == null) {
                throw Exception("Failed to generate story")
            }

            // Wait for 20 seconds before generating title
            delay(10_000L)

            // Now generate the title
            isGeneratingTitle = true
            val titlePrompt =
                "Given this story about $personName, generate a only one compelling and concise title (maximum 10 words) that captures the essence of their journey. Story: $story"

            val title = geminiService.makeGeminiApiRequest(titlePrompt)
            if (title == null) {
                throw Exception("Failed to generate title")
            }

            val newStory = Story(
                person = personName,
                title = title.trim(),
                story = story,
                timestamp = Timestamp.now()
            )

            db.collection("stories").add(newStory).await()
            personInput = ""
            Toast.makeText(context, "Story and title added successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            error = "Error adding story: ${e.message}"
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
            isGeneratingTitle = false
        }
    }

    // Function to toggle person list visibility
    fun togglePersonList() {
        showPersonList = !showPersonList
        if (!showPersonList) {
            searchQuery = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackgroundTheme)
            .padding(16.dp)
    ) {
        // Statistics Card
        StoryStatCard(
            title = "Total Stories",
            value = totalStories.toString(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = personInput,
                    onValueChange = { personInput = it },
                    placeholder = {
                        Text(
                            "Enter Business Personality Name",
                            color = OnboardingTextSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Follow,
                        unfocusedBorderColor = OnboardingTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (personInput.isNotBlank()) {
                            scope.launch { addStory(personInput) }
                        } else {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Follow,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Add Story")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Show/Hide Persons Button
        Button(
            onClick = { togglePersonList() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Melrose
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(if (showPersonList) "Hide All Persons" else "Show All Persons")
        }

        // Search and List Section (only shown when showPersonList is true)
        if (showPersonList) {

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Persons", color = OnboardingTextSecondary) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Persons List
            Card(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .border(
                        width = 1.dp,
                        color = OnboardingTextSecondary,
                        shape = RoundedCornerShape(4.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(filteredPersons) { person ->
                        Text(
                            text = person,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Divider(color = OnboardingTextSecondary)
                    }
                }
            }
        }

        // Loading States
        if (isLoading || isGeneratingTitle) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Follow,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isGeneratingTitle) "Generating title..." else "Generating story...",
                        color = Color.White
                    )
                }
            }
        }

        // Error Message
        error?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun StoryStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = OnboardingTextSecondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
} 