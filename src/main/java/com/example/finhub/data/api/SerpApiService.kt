package com.example.finhub.data.api

import com.example.finhub.data.model.SerpApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SerpApiService {
    @GET("search.json")
    suspend fun getGoogleNews(
        @Query("engine") engine: String = "google_news",
        @Query("q") query: String,
        @Query("gl") country: String,
        @Query("hl") language: String = "en",
        @Query("api_key") apiKey: String
    ): SerpApiResponse
} 