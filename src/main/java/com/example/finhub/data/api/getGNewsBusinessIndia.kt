package com.example.finhub.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface GNewsApiService {

    @GET("search")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("country") country: String,
        @Query("lang") lang: String,
        @Query("token") token: String
    ): GNewsResponse

    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String,
        @Query("lang") lang: String,
        @Query("topic") topic: String,
        @Query("token") token: String
    ): GNewsResponse
}

// Data classes for GNews API
data class GNewsResponse(
    val totalArticles: Int,
    val articles: List<GNewsArticle>
)

data class GNewsArticle(
    val title: String,
    val description: String?,
    val content: String?,
    val url: String,
    val image: String?,
    val publishedAt: String,
    val source: GNewsSource
)

data class GNewsSource(
    val name: String,
    val url: String
)
