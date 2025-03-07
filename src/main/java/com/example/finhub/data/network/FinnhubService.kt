package com.example.finhub.data.network

import com.example.finhub.data.model.NewsArticle
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubService {
    @GET("news")
    suspend fun getBusinessNews(
        @Query("category") category: String = "general",
        @Query("token") token: String // No hardcoded token here
    ): List<NewsArticle>
}
