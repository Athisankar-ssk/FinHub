package com.example.finhub.ui.home

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finhub.data.network.ApiClient
import com.example.finhub.data.network.FirebaseService
import com.example.finhub.data.network.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.Collections

@RequiresApi(Build.VERSION_CODES.O)
class NewsViewModel(
    private val finnhubApiKey: String,
    private val newsApiKey: String,
    private val context: Context
) : ViewModel() {

    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles

    private val finnhubService = ApiClient.finnhubService
    private val newsApiService = ApiClient.newsApi
    private val firebaseService = FirebaseService(context)

    init {
        fetchAndStoreNews()
        loadArticlesFromFirestore()
    }

    private val processedUrls = Collections.synchronizedSet(HashSet<String>())

    private fun fetchAndStoreNews() {
        viewModelScope.launch {
            try {
                // First fetch all news without processing
                val finnhubNews = fetchFinnhubFinanceNews()
                val newsApiNews = fetchNewsApiBusinessNews()

                // Combine all news sources
                val allNews = finnhubNews + newsApiNews
                Log.d("NewsViewModel", "Fetched ${allNews.size} total articles")

                // Process sequentially on a single thread to avoid race conditions
                withContext(Dispatchers.IO) {
                    val uniqueArticles = mutableListOf<NewsArticle>()

                    // First filter out duplicates
                    allNews.forEach { article ->
                        article.url?.let { url ->
                            if (url.isNotEmpty() && !processedUrls.contains(url)) {
                                processedUrls.add(url)
                                uniqueArticles.add(article)
                            } else {
                                Log.d("NewsViewModel", "Filtered duplicate URL before processing: $url")
                            }
                        }
                    }

                    Log.d("NewsViewModel", "Processing ${uniqueArticles.size} unique articles")

                    // Now process the unique articles
                    uniqueArticles.forEach { article ->
                        val docId = firebaseService.storeArticle(article)
                        if (docId != null) {
                            Log.d("NewsViewModel", "Stored article: ${article.headline}, ID: $docId")
                        } else {
                            Log.d("NewsViewModel", "Skipped article: ${article.headline} (already exists or scraping failed)")
                        }
                    }
                }

                // Refresh the displayed articles after all processing is complete
                loadArticlesFromFirestore()
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error in fetchAndStoreNews: ${e.message}")
            }
        }
    }    private fun loadArticlesFromFirestore() {
        viewModelScope.launch {
            val articles = firebaseService.getArticles()
            _newsArticles.value = articles
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
            Log.e("NewsViewModel", "Error fetching Finnhub news: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchNewsApiBusinessNews(): List<NewsArticle> {
        return try {
            val response = newsApiService.getBusinessNews(category = "business", apiKey = newsApiKey)
            Log.d("NewsViewModel", "NewsAPI.org Response: $response")
            response.articles.map { article ->
                NewsArticle(
                    headline = article.title,
                    image = article.urlToImage ?: "",
                    source = article.source.name,
                    datetime = firebaseService.parseNewsApiDate(article.publishedAt),
                    summary = article.description ?: "",
                    url = article.url
                )
            }
        } catch (e: Exception) {
            Log.e("NewsViewModel", "Error fetching NewsAPI.org news: ${e.message}")
            emptyList()
        }
    }

    companion object {
        fun provideFactory(finnhubApiKey: String, newsApiKey: String, context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return NewsViewModel(finnhubApiKey, newsApiKey, context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}