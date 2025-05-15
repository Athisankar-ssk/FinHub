package com.example.finhub.admin

import android.content.Context
import android.util.Log
import com.example.finhub.data.api.ApiClient
import com.example.finhub.data.database.FirebaseService
import com.example.finhub.data.model.NewsArticle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar

class NewsDataCollector(
    private val finnhubApiKey: String,
    private val newsApiKey: String,
    private val GnewsApiKey: String,
    private val serpApiKey: String,
    private val context: Context
) {
    private val finnhubService = ApiClient.finnhubService
    private val newsApiService = ApiClient.newsApi
    private val gNewsApiService = ApiClient.gNewsApi
    private val serpApiService = ApiClient.serpApi
    private val firebaseService = FirebaseService(context)
    private val processedUrls = Collections.synchronizedSet(HashSet<String>())
    private var progressCallback: ((module: String, current: Int, total: Int, articleTitle: String) -> Unit)? = null
    
    val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date())

    fun setProgressCallback(callback: (module: String, current: Int, total: Int, articleTitle: String) -> Unit) {
        progressCallback = callback
    }

    private suspend fun initializeProcessedUrls() = coroutineScope {
        // Clear processedUrls
        processedUrls.clear()
        // Load processed URLs from Firestore
        FirebaseFirestore.getInstance().collection("articles")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.getString("url")?.let { url ->
                        processedUrls.add(url)
                    }
                }
                Log.d("NewsDataCollector", "Loaded ${processedUrls.size} URLs from Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("NewsDataCollector", "Error loading URLs from Firestore: ${e.message}")
            }
    }

    suspend fun collectAndStoreNews() = coroutineScope {
        initializeProcessedUrls();
        Log.d("NewsDataCollector", "Loaded ${processedUrls.size} URLs from Firestore")
        try {
            progressCallback?.invoke("SERP Daily", 0, 1, "Starting SERP Daily Finance fetch...")

            val serpNewsFinance = fetchSerpNews("today indian finance", "daily finance", "india")
            val serpNewsBusiness = fetchSerpNews("today indian business", "daily business", "india")

            // Create a calendar instance for date manipulation
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val collectedFinanceArticles = mutableListOf<NewsArticle>()
            val collectedBusinessArticles = mutableListOf<NewsArticle>()
            
            // Try for today and yesterday only
            for (daysBack in 0..1) {
                val currentDate = dateFormat.format(calendar.time)
                Log.d("NewsDataCollector", "Checking articles for date: $currentDate")
                
                // Filter and collect finance articles
                if (collectedFinanceArticles.size < 15) {
                    val financeArticlesForDay = serpNewsFinance.filter { article ->
                        article.datetime == currentDate
                    }
                    Log.d("NewsDataCollector", "Found ${financeArticlesForDay.size} finance articles for $currentDate")
                    
                    val remainingFinanceNeeded = 15 - collectedFinanceArticles.size
                    collectedFinanceArticles.addAll(financeArticlesForDay.take(remainingFinanceNeeded))
                }
                
                // Filter and collect business articles
                if (collectedBusinessArticles.size < 15) {
                    val businessArticlesForDay = serpNewsBusiness.filter { article ->
                        article.datetime == currentDate
                    }
                    Log.d("NewsDataCollector", "Found ${businessArticlesForDay.size} business articles for $currentDate")
                    
                    val remainingBusinessNeeded = 15 - collectedBusinessArticles.size
                    collectedBusinessArticles.addAll(businessArticlesForDay.take(remainingBusinessNeeded))
                }
                
                // If both categories have 15 articles, we can break early
                if (collectedFinanceArticles.size >= 15 && collectedBusinessArticles.size >= 15) {
                    break
                }
                
                // Move to previous day
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            val totalArticles = collectedFinanceArticles + collectedBusinessArticles
            Log.d("NewsDataCollector", "Total collected articles: ${totalArticles.size} (Finance: ${collectedFinanceArticles.size}, Business: ${collectedBusinessArticles.size})")

            // Process all articles together
            totalArticles.forEachIndexed { index, article ->
                try {
                    val category = if (article in collectedFinanceArticles) "Finance" else "Business"
                    progressCallback?.invoke("SERP Daily", index + 1, totalArticles.size, "$category: ${article.headline}")

                    article.url?.let { url ->
                        if (url.isNotEmpty() && !processedUrls.contains(url)) {
                            processedUrls.add(url)
                            val docId = firebaseService.storeArticle(article)
                            if (docId != null) {
                                Log.d("NewsDataCollector", "Successfully stored $category article: ${article.headline}")
                            } else {
                                Log.e("NewsDataCollector", "Failed to store $category article: ${article.headline}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Daily module: ${e.message}")
            e.printStackTrace()
        }
//
//        try {
//
//            // Signal start of GNews module
//            progressCallback?.invoke("GNews", 0, 1, "Starting GNews fetch...")
//
//            // MODULE 1: GNEWS
//            val allNews = fetchGNewsBusinessIndia("daily finance","india") +
//                         fetchGNewsBusinessIndia("daily business", "india")
//            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles from GNews")
//
//            val uniqueArticles = mutableListOf<NewsArticle>()
//            // Filter out duplicates
//            allNews.forEach { article ->
//                article.url?.let { url ->
//                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
//                        processedUrls.add(url)
//                        uniqueArticles.add(article)
//                    } else {
//                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
//                    }
//                }
//            }
//
//            Log.d("NewsDataCollector", "Processing ${uniqueArticles.size} unique GNews articles")
//
//            // Process each unique article sequentially
//            uniqueArticles.forEachIndexed { index, article ->
//                try {
//                    // Update progress for GNews module
//                    progressCallback?.invoke("GNews", index + 1, uniqueArticles.size, article.headline)
//
//                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
//                    val docId = firebaseService.storeArticle(article)
//                    if (docId != null) {
//                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
//                    } else {
//                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
//                    }
//                } catch (e: Exception) {
//                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
//                }
//            }
//
//        } catch (e: Exception) {
//            Log.e("NewsDataCollector", "Error in GNews module: ${e.message}")
//            e.printStackTrace()
//        }
//
//
////        MODULE 2: NEWS API
//
//        progressCallback?.invoke("SERP", 0, 1, "Starting NEWS API fetch...")
//
//        try {
//            // First fetch all news without processing
//            val allNews = fetchNewsApiBusinessNews("cryptocurrency", "india", "cryptocurrency", false) +
//                    fetchNewsApiBusinessNews("indian mutual funds", "india", "mutual funds", false) +
//                    fetchNewsApiBusinessNews("indian tax and budgeting", "india", "tax and budgeting", false) +
//                    fetchNewsApiBusinessNews("indian banking and insurance", "india", "banking and insurance", false) +
//                    fetchNewsApiBusinessNews("indian fintech", "india", "fintech", false)
//
//            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")
//
//            val uniqueArticles = mutableListOf<NewsArticle>()
//            // Filter out duplicates
//            allNews.forEach { article ->
//                article.url?.let { url ->
//                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
//                        processedUrls.add(url)
//                        uniqueArticles.add(article)
//                    } else {
//                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
//                    }
//                }
//            }
//
//            Log.d("NewsDataCollector", "Processing ${uniqueArticles.size} unique articles")
//
//            // Process each unique article sequentially
//            uniqueArticles.forEachIndexed { index, article ->
//                try {
//                    // Update progress for SERP module
//                    progressCallback?.invoke("SERP", index + 1, uniqueArticles.size, article.headline)
//
//                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
//                    val docId = firebaseService.storeArticle(article)
//                    if (docId != null) {
//                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
//                    } else {
//                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
//                    }
//                } catch (e: Exception) {
//                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
//                }
//            }
//
//        } catch (e: Exception) {
//            Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
//            e.printStackTrace()
//        }
//
//        // Reset progress for new module
//        progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
//
//        // MODULE 3: SERP API
//        try {
//            val serpNews = fetchSerpNews("business", "business", "india")
//            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")
//
//            val uniqueSerpArticles = mutableListOf<NewsArticle>()
//            // Filter out duplicates
//            serpNews.forEach { article ->
//                article.url?.let { url ->
//                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
//                        processedUrls.add(url)
//                        uniqueSerpArticles.add(article)
//                    } else {
//                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
//                    }
//                }
//            }
//
//            Log.d("NewsDataCollector", "Processing ${uniqueSerpArticles.size} unique SERP articles")
//
//            // Process each unique article sequentially
//            uniqueSerpArticles.forEachIndexed { index, article ->
//                try {
//                    // Update progress for SERP module
//                    progressCallback?.invoke("SERP", index + 1, uniqueSerpArticles.size, article.headline)
//
//                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
//                    val docId = firebaseService.storeArticle(article)
//                    if (docId != null) {
//                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
//                    } else {
//                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
//                    }
//                } catch (e: Exception) {
//                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
//            e.printStackTrace()
//            throw e
//        }

        // MODULE 4: SERP DAILY FINANCE

    }

    private suspend fun fetchFinnhubFinanceNews(): List<NewsArticle> {
        return try {
            finnhubService.getBusinessNews(category = "finance", token = finnhubApiKey)
                .map { article ->
                    NewsArticle(
                        headline = article.headline,
                        image = article.image,
                        source = article.source,
                        datetime = article.datetime,
                        summary = article.summary,
                        url = article.url
                    )
                }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error fetching Finnhub news: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchNewsApiBusinessNews(search: String, re: String, cat: String, bus: Boolean): List<NewsArticle> {
        return try {
            val response = if (bus) {
                newsApiService.getBusinessNews(category = search, apiKey = newsApiKey)
            } else {
                // No need to replace anything, use the query as is
                Log.d("NewsDataCollector", "Making NewsAPI request with query: $search")
                newsApiService.getCustomNews(query = search, apiKey = newsApiKey)
            }

            Log.d("NewsDataCollector", "NewsAPI.org Response: $response")
            
            if (response.articles.isEmpty()) {
                Log.w("NewsDataCollector", "No articles found for query: $search")
                return emptyList()
            }

            response.articles.map { article ->
                NewsArticle(
                    headline = article.title,
                    image = article.urlToImage ?: "",
                    source = article.source.name,
                    datetime = formatToDateOnly(article.publishedAt),
                    summary = article.description ?: "",
                    url = article.url,
                    category = cat,
                    region = re,
                    savedDate = currentDate
                )
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error fetching NewsAPI.org news for query '$search': ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun fetchGNewsBusinessIndia(query: String, country: String): List<NewsArticle> {
        return try {
            val response = gNewsApiService.searchNews(
                query = query,
                country = country,
                lang = "en",
                token = GnewsApiKey
            )
            Log.d("NewsDataCollector", "GNews Response: $response")
            response.articles.map { article ->
                NewsArticle(
                    headline = article.title,
                    image = article.image ?: "",
                    source = article.source.name,
                    datetime = formatToDateOnly(article.publishedAt),
                    summary = article.description ?: "",
                    url = article.url,
                    category = query,  // Using query as category
                    region = country,
                    savedDate = currentDate
                )
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error fetching GNews India business: ${e.message}")
            emptyList()
        }
    }

    private fun formatToDateOnly(isoDateTime: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoDateTime ?: "")
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            outputFormat.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatSerpDate(serpDateTime: String?): String {
        return try {
            if (serpDateTime == null) return ""

            // Remove " UTC" (with leading space) so we're left with "+0000"
            val cleanedDate = serpDateTime.replace(" UTC", "").trim()

            val inputFormat = SimpleDateFormat("MM/dd/yyyy, hh:mm a, Z", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val date = inputFormat.parse(cleanedDate)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            outputFormat.format(date!!)
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error parsing SerpAPI date '$serpDateTime': ${e.message}")
            ""
        }
    }

    private suspend fun fetchSerpNews(query: String, cat: String, re: String): List<NewsArticle> {
        return try {
            val response = serpApiService.getGoogleNews(query = query, country = "in",language = "en", apiKey = serpApiKey)
            
            val filteredArticles = response.news_results
            
            Log.d("NewsDataCollector", "After filtering: ${filteredArticles.size} valid articles")
            
            val mappedArticles = filteredArticles.map { article ->
                try {
                    val newsArticle = NewsArticle(
                        headline = article.title,
                        image = article.thumbnail ?: "",
                        source = article.source?.name ?: "Unknown Source",
                        datetime = formatSerpDate(article.date),
                        summary = article.snippet ?: "No summary available",
                        url = article.link,
                        category = cat,
                        region = re,
                        savedDate = currentDate
                    )
                    Log.d("NewsDataCollector", "Successfully mapped article: ${newsArticle.headline}")
                    newsArticle
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error mapping article ${article.title}: ${e.message}")
                    null
                }
            }.filterNotNull()
            
            Log.d("NewsDataCollector", "Final mapped articles: ${mappedArticles.size}")
            mappedArticles
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error fetching SerpAPI news for query '$query': ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}

