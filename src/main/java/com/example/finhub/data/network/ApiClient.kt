package com.example.finhub.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val FINNHUB_BASE_URL = "https://finnhub.io/api/v1/"
    private const val NEWSAPI_BASE_URL = "https://newsapi.org/"

    // Interceptor to add User-Agent header for NewsAPI.org
    private val newsApiInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "PostmanRuntime/7.36.3") // Replace with your Postman User-Agent if different
            .build()
        chain.proceed(requestWithUserAgent)
    }

    // OkHttpClient with the interceptor for NewsAPI.org
    private val newsApiClient = OkHttpClient.Builder()
        .addInterceptor(newsApiInterceptor)
        .build()

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
            .client(newsApiClient) // Use the OkHttpClient with the User-Agent interceptor
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }
}
