package com.example.finhub.data.network

import com.example.finhub.data.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getBusinessNews(
        @Query("country") country: String = "in",  // Adjust country if needed
        @Query("category") category: String = "business",
        @Query("apiKey") apiKey: String
    ): NewsResponse
}
