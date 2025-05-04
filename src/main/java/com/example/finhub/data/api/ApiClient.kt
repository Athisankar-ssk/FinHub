package com.example.finhub.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val FINNHUB_BASE_URL = "https://finnhub.io/api/v1/"
    private const val NEWSAPI_BASE_URL = "https://newsapi.org/"
    private const val GNEWS_BASE_URL = "https://gnews.io/api/v4/"
    private const val SERP_BASE_URL = "https://serpapi.com/"

    // Interceptor to add User-Agent header for NewsAPI.org
    private val newsApiInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "PostmanRuntime/7.36.3") // Replace with your User-Agent if needed
            .build()
        chain.proceed(requestWithUserAgent)
    }

    private val newsApiClient = OkHttpClient.Builder()
        .addInterceptor(newsApiInterceptor)
        .build()

    // Add logging interceptor for SerpAPI

    val finnhubService: FinnhubService by lazy {
        Retrofit.Builder()
            .baseUrl(FINNHUB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubService::class.java)
    }

    val newsApi: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(NEWSAPI_BASE_URL)
            .client(newsApiClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }

    val gNewsApi: GNewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GNEWS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GNewsApiService::class.java)
    }

    val serpApi: SerpApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SERP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SerpApiService::class.java)
    }
}
