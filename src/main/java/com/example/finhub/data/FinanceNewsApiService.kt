package com.example.finhub.data

import retrofit2.http.GET
import retrofit2.http.Query

interface FinanceNewsApiService {

    @GET("news")
    suspend fun getFinanceNews(
        @Query("category") category: String = "general",
        @Query("token") token: String = "cuslsapr01qnihs7d1lgcuslsapr01qnihs7d1m0"
    ): List<FinanceNewsArticle>
}

// Data class for Finnhub News Article
data class FinanceNewsArticle(
    val headline: String,
    val summary: String,
    val image: String,
    val source: String,
    val datetime: Long
)
