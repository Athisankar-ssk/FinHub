package com.example.finhub.ui.screens.home

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finhub.data.api.ApiClient
import com.example.finhub.data.database.FirebaseService
import com.example.finhub.data.model.NewsArticle
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class NewsFetchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val finnhubApiKey = inputData.getString("cuslsapr01qnihs7d1lgcuslsapr01qnihs7d1m0") ?: ""
    private val newsApiKey = inputData.getString("cda48b3dfe61437492014dec4ef5a703") ?: ""
    private val finnhubService = ApiClient.finnhubService
    private val newsApiService = ApiClient.newsApi
    private val firebaseService = FirebaseService(context)

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result = coroutineScope {
        try {
            val finnhubNewsDeferred = async { fetchFinnhubFinanceNews() }
            val newsApiBusinessNewsDeferred = async { fetchNewsApiBusinessNews() }

            val finnhubNews = finnhubNewsDeferred.await().take(20)
            val newsApiNews = newsApiBusinessNewsDeferred.await().take(20)

            (finnhubNews + newsApiNews).forEach { article ->
                val docId = firebaseService.storeArticle(article)
                if (docId != null) {
                    Log.d("NewsFetchWorker", "Stored article: ${article.headline}, ID: $docId")
                } else {
                    Log.e("NewsFetchWorker", "Failed to store article: ${article.headline}")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("NewsFetchWorker", "Error fetching/storing news: ${e.message}")
            Result.retry()
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
            Log.e("NewsFetchWorker", "Error fetching Finnhub news: ${e.message}")
            emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchNewsApiBusinessNews(): List<NewsArticle> {
        return try {
            val response = newsApiService.getBusinessNews(category = "business", apiKey = newsApiKey)
            response.articles.map { article ->
                NewsArticle(
                    headline = article.title,
                    image = article.urlToImage ?: "",
                    source = article.source.name,
                    datetime = article.publishedAt,
                    summary = article.description ?: "",
                    url = article.url
                )
            }
        } catch (e: Exception) {
            Log.e("NewsFetchWorker", "Error fetching NewsAPI news: ${e.message}")
            emptyList()
        }
    }
}