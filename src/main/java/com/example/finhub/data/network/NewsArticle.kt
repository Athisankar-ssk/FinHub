package com.example.finhub.data.network

data class NewsArticle(
    val id: String? = null, // Firestore document ID
    val headline: String,
    val image: String,
    val source: String,
    val datetime: Long,
    val summary: String,
    val url: String? = null,
    val content: String? = null // Scraped article content
)