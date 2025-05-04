package com.example.finhub.data.service

import android.content.Context
import android.util.Log
import com.example.finhub.data.api.ApiClient
import com.example.finhub.data.database.FirebaseService
import com.example.finhub.data.model.NewsArticle
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date())

    fun collectAndStoreNews() {
        runBlocking {
//            try {
//                // First fetch all news without processing
//                val allNews = fetchGNewsBusinessIndia("finance","india") + fetchGNewsBusinessIndia("business", "india")
//                Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")
//
//                val uniqueArticles = mutableListOf<NewsArticle>()
//                // Filter out duplicates
//                allNews.forEach { article ->
//                    article.url?.let { url ->
//                        if (url.isNotEmpty() && !processedUrls.contains(url)) {
//                            processedUrls.add(url)
//                            uniqueArticles.add(article)
//                        } else {
//                            Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
//                        }
//                    }
//                }
//
//                Log.d("NewsDataCollector", "Processing ${uniqueArticles.size} unique articles")
//
//                // Process each unique article sequentially
//                for (article in uniqueArticles) {
//                    try {
//                        Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
//                        val docId = firebaseService.storeArticle(article)
//                        if (docId != null) {
//                            Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
//                        } else {
//                            Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
//                        }
//                    } catch (e: Exception) {
//                        Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
//                    }
//                }
//
//            } catch (e: Exception) {
//                Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
//                e.printStackTrace()
//            }
//            try {
//                // First fetch all news without processing
//                val allNews = fetchNewsApiBusinessNews("cryptocurrency", "india", "cryptocurrency", false) +
//                        fetchNewsApiBusinessNews("indian mutual funds", "india", "mutual funds", false) +
//                        fetchNewsApiBusinessNews("indian tax and budgeting", "india", "tax and budgeting", false) +
//                        fetchNewsApiBusinessNews("indian banking and insurance", "india", "banking and insurance", false) +
//                        fetchNewsApiBusinessNews("indian fintech", "india", "fintech", false)
//
//                Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")
//
//                val uniqueArticles = mutableListOf<NewsArticle>()
//                // Filter out duplicates
//                allNews.forEach { article ->
//                    article.url?.let { url ->
//                        if (url.isNotEmpty() && !processedUrls.contains(url)) {
//                            processedUrls.add(url)
//                            uniqueArticles.add(article)
//                        } else {
//                            Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
//                        }
//                    }
//                }
//
//                Log.d("NewsDataCollector", "Processing ${uniqueArticles.size} unique articles")
//
//                // Process each unique article sequentially
//                for (article in uniqueArticles) {
//                    try {
//                        Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
//                        val docId = firebaseService.storeArticle(article)
//                        if (docId != null) {
//                            Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
//                        } else {
//                            Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
//                        }
//                    } catch (e: Exception) {
//                        Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
//                    }
//                }
//
//            } catch (e: Exception) {
//                Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
//                e.printStackTrace()
//            }

            try {
                // First fetch all news without processing
                val allNews = fetchSerpNews("stock market", "stock market","india")

                Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

                val uniqueArticles = mutableListOf<NewsArticle>()
                // Filter out duplicates
                allNews.forEach { article ->
                    article.url?.let { url ->
                        if (url.isNotEmpty() && !processedUrls.contains(url)) {
                            processedUrls.add(url)
                            uniqueArticles.add(article)
                        } else {
                            Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                        }
                    }
                }

                Log.d("NewsDataCollector", "Processing ${uniqueArticles.size} unique articles")

                // Process each unique article sequentially
                for (article in uniqueArticles) {
                    try {
                        Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                        val docId = firebaseService.storeArticle(article)
                        if (docId != null) {
                            Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                        } else {
                            Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                        }
                    } catch (e: Exception) {
                        Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
                e.printStackTrace()
            }


        }
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

    private suspend fun fetchGNewsBusinessIndia(cat: String, re: String): List<NewsArticle> {
        return try {
            val response = gNewsApiService.getBusinessNewsIndia(country = re, lang = "en", topic = cat, token = GnewsApiKey )
            Log.d("NewsDataCollector", "GNews Response: $response")
            response.articles.map { article ->
                NewsArticle(
                    headline = article.title,
                    image = article.image ?: "",
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