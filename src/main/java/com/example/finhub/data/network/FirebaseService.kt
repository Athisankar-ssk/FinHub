package com.example.finhub.data.network

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class FirebaseService(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val articlesCollection = db.collection("articles")

    // Scrape full content from article URL
    private suspend fun scrapeArticleContent(url: String): String? {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            println("No network available for scraping $url")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                println("Scraping URL: $url")
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(15000) // Increased timeout
                    .get()

                val articleText = StringBuilder()
                val selectors = listOf(
                    "article",
                    ".article-content",
                    ".article-body",
                    ".story-body",
                    ".entry-content",
                    "main",
                    "div[class*='article']",
                    "div[class*='content']",
                    "div[itemprop='articleBody']",
                    "section[class*='article']"
                )

                // Try specific article containers
                for (selector in selectors) {
                    val elements = doc.select(selector)
                    if (elements.isNotEmpty()) {
                        val paragraphs = elements.first()?.select("p") ?: continue
                        paragraphs.forEach { paragraph ->
                            val text = paragraph.text()
                            if (text.length > 40 &&
                                !text.contains("advertisement", ignoreCase = true) &&
                                !paragraph.hasClass("caption") &&
                                !paragraph.hasClass("footer")) {
                                articleText.append(text).append("\n\n")
                            }
                        }
                        if (articleText.isNotEmpty()) break
                    }
                }

                // Fallback: collect all paragraphs
                if (articleText.isEmpty()) {
                    val paragraphs = doc.select("p")
                    paragraphs.forEach { paragraph ->
                        val text = paragraph.text()
                        if (text.length > 40 &&
                            !text.contains("advertisement", ignoreCase = true) &&
                            !paragraph.hasClass("caption") &&
                            !paragraph.hasClass("footer")) {
                            articleText.append(text).append("\n\n")
                        }
                    }
                }

                val result = articleText.toString().trim()
                if (result.isEmpty()) {
                    println("No content found for $url")
                    null
                } else {
                    result
                }
            } catch (e: Exception) {
                println("Error scraping $url: ${e.javaClass.simpleName} - ${e.message}")
                null
            }
        }
    }

    // Store a news article in Firestore with scraped content
    suspend fun storeArticle(article: NewsArticle): String? {
        try {
            // Skip articles with no URL
            if (article.url.isNullOrEmpty()) {
                println("Skipping article with no URL: ${article.headline}")
                return null
            }

            // Generate consistent document ID from URL
            val docId = article.url.hashCode().toString()

            // Check if article already exists by document ID
            val docRef = articlesCollection.document(docId)
            val docSnapshot = docRef.get().await()

            if (docSnapshot.exists()) {
                println("Article already exists in Firestore: ${article.headline}")
                return docId
            }

            // Scrape the full content
            val content = scrapeArticleContent(article.url)

            // Only store if content was successfully scraped
            if (content.isNullOrEmpty()) {
                println("Skipping article with no content: ${article.headline}")
                return null
            }

            // Convert article to a Firestore-compatible map
            val articleData = hashMapOf(
                "headline" to article.headline,
                "image" to article.image,
                "source" to article.source,
                "datetime" to article.datetime,
                "summary" to article.summary,
                "url" to article.url,
                "content" to content
            )

            // Store using document ID based on URL hash
            docRef.set(articleData).await()
            return docId
        } catch (e: Exception) {
            println("Error storing article: ${e.message}")
            return null
        }
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
                        datetime = doc.getLong("datetime") ?: 0L,
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
    @RequiresApi(Build.VERSION_CODES.O)
    fun parseNewsApiDate(dateString: String): Long {
        return try {
            OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toEpochSecond()
        } catch (e: Exception) {
            Instant.now().epochSecond
        }
    }
}