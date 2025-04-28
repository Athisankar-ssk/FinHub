package com.example.finhub.data.network

import com.example.finhub.data.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getBusinessNews(
        @Query("category") category: String = "business",
        @Header("X-Api-Key") apiKey: String // Sending API key as a header
    ): NewsResponse
}