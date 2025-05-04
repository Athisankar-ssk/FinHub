package com.example.finhub.data.api

import com.example.finhub.data.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getBusinessNews(
        @Query("category") category: String,
        @Header("X-Api-Key") apiKey: String // Sending API key as a header
    ): NewsResponse

    @GET("v2/everything")
    suspend fun getCustomNews(
        @Query("q") query: String,
        @Query("language") language: String = "en",
        @Header("X-Api-Key") apiKey: String
    ): NewsResponse
}