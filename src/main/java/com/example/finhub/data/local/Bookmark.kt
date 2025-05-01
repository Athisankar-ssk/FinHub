package com.example.finhub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.finhub.data.network.NewsArticle

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey
    val url: String,
    val headline: String,
    val image: String,
    val source: String,
    val datetime: String,
    val summary: String,
    val content: String?
) {
    companion object {
        fun fromNewsArticle(article: NewsArticle): Bookmark {
            return Bookmark(
                url = article.url ?: "",
                headline = article.headline,
                image = article.image,
                source = article.source,
                datetime = article.datetime,
                summary = article.summary,
                content = article.content
            )
        }
    }

    fun toNewsArticle(): NewsArticle {
        return NewsArticle(
            headline = headline,
            image = image,
            source = source,
            datetime = datetime,
            summary = summary,
            url = url,
            content = content
        )
    }
} 