package com.example.finhub.data.network

data class NewsArticle(
    val id: String? = null,
    val headline: String,
    val image: String,
    val source: String,
    val datetime: String,
    val summary: String,
    val url: String? = null,
    val content: String? = null
)