package com.example.finhub.data.network

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.min

class FirebaseService(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val articlesCollection = db.collection("articles")
    private val geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-001:generateContent?key=AIzaSyC53aFcBdKUmDJWdRnryl4xakEyYniCKeg"

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

    // Make a request to Gemini AI API with retries
    private suspend fun makeGeminiApiRequest(prompt: String, retries: Int = 3): String? {
        return withContext(Dispatchers.IO) {
            var attempt = 0
            while (attempt < retries) {
                // Wait for rate limit before making request
                waitForRateLimit()

                try {
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        println("No network available for Gemini API request")
                        return@withContext null
                    }

                    println("Sending Gemini API request (attempt ${attempt + 1}): $prompt")
                    val jsonBody = """
                        {
                            "contents": [
                                {
                                    "parts": [
                                        {
                                            "text": "$prompt"
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

    // Store a news article in Firestore with AI-generated content, title, and summary
    suspend fun storeArticle(article: NewsArticle): String? {
        try {
            // Skip articles with no URL
            if (article.url.isNullOrEmpty()) {
                println("Skipping article with no URL: ${article.headline}")
                return null
            }

            // Generate consistent document ID from URL
            val docId = article.url.hashCode().toString()

            // Check if article with the same URL already exists in Firestore
            val querySnapshot = articlesCollection
                .whereEqualTo("url", article.url)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                println("Article with URL already exists in Firestore: ${article.url}")
                return docId
            }

            println("Processing article: ${article.headline}")

            // Make three Gemini API requests sequentially
            val content = makeGeminiApiRequest(
                "Read the full article at ${article.url} and rewrite it in simple English. Provide only the rewritten article text without any introduction, headings, or markdown formatting except new line character."
            )
            if (content.isNullOrEmpty()) {
                println("Failed to get content for: ${article.headline}")
                return null
            }

            val title = makeGeminiApiRequest(
                "Read the article at ${article.url} and generate a concise and engaging news headline (max 10 words). Provide only the headline text without any introduction, headings, or markdown formatting."
            )
            if (title.isNullOrEmpty()) {
                println("Failed to get title for: ${article.headline}")
                return null
            }

            val summary = makeGeminiApiRequest(
                "Read the article at ${article.url} and summarize it in exactly 50-60 words. Provide only the summary text without any introduction, headings, or markdown formatting."
            )
            if (summary.isNullOrEmpty()) {
                println("Failed to get summary for: ${article.headline}")
                return null
            }

            var image = article.image
            if(image == "https://static2.finnhub.io/file/publicdatany/finnhubimage/market_watch_logo.png"){
                image = ""
            }

            // Convert article to a Firestore-compatible map
            val articleData = hashMapOf(
                "headline" to title,
                "image" to image,
                "source" to article.source,
                "datetime" to article.datetime,
                "summary" to summary,
                "url" to article.url,
                "content" to content
            )

            // Store using document ID based on URL hash
            val docRef = articlesCollection.document(docId)
            docRef.set(articleData).await()
            println("Stored article: $title, ID: $docId")
            return docId
        } catch (e: Exception) {
            println("Error storing article: ${e.message}")
            return null
        }
    }

    // Function to process a list of NewsArticle and store them sequentially
    suspend fun processAndStoreArticles(articles: List<NewsArticle>) {
        for (article in articles) {
            storeArticle(article)
        }
        println("Finished processing all articles.")
    }

    // Fetch all articles from Firestore, sorted by datetime descending
    suspend fun getArticles(): List<NewsArticle> {
        return try {
            val snapshot = articlesCollection
                .orderBy("datetime", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    NewsArticle(
                        id = doc.id,
                        headline = doc.getString("headline") ?: "",
                        image = doc.getString("image") ?: "",
                        source = doc.getString("source") ?: "",
                        datetime = doc.getString("datetime") ?: "",
                        summary = doc.getString("summary") ?: "",
                        url = doc.getString("url"),
                        content = doc.getString("content")
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching articles: ${e.message}")
            emptyList()
        }
    }

    // Parse NewsAPI date to epoch seconds
}