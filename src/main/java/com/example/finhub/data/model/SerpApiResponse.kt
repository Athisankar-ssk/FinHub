package com.example.finhub.data.model

data class SerpApiResponse(
    val news_results: List<SerpNewsArticle>,
    val search_parameters: SearchParameters,
    val search_information: SearchInformation
)

data class SerpNewsArticle(
    val title: String,
    val link: String,
    val source: SerpSource,
    val date: String,
    val snippet: String,
    val thumbnail: String? = null
)

data class SerpSource(
    val name: String,
    val icon: String? = null,
    val authors: List<String>? = null
)

data class SearchParameters(
    val engine: String,
    val q: String,
    val gl: String,
    val hl: String
)

data class SearchInformation(
    val total_results: Long,
    val time_taken_displayed: Double
) 