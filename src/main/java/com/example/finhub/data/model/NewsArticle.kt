package com.example.finhub.data.model

data class NewsArticle(
    val id: String? = null,
    val headline: String,
    val image: String,
    val source: String,
    val datetime: String,
    val summary: String,
    val url: String? = null,
    val content: String? = null,
    val region: String = "global",     
    val category: String = "general",  
    val savedDate: String = ""         
)