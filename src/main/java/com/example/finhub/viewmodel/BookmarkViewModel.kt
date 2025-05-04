package com.example.finhub.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finhub.data.local.AppDatabase
import com.example.finhub.data.local.Bookmark
import com.example.finhub.data.model.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookmarkViewModel(context: Context) : ViewModel() {
    private val database = AppDatabase.getDatabase(context)
    private val bookmarkDao = database.bookmarkDao()

    private val _bookmarks = MutableStateFlow<List<NewsArticle>>(emptyList())
    val bookmarks: StateFlow<List<NewsArticle>> = _bookmarks.asStateFlow()

    // Keep track of bookmarked status
    private val bookmarkedArticles = mutableStateMapOf<String, Boolean>()

    init {
        loadBookmarks()
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            bookmarkDao.getAllBookmarks().collect { bookmarks ->
                _bookmarks.value = bookmarks.map { it.toNewsArticle() }
                // Update bookmarked status
                bookmarks.forEach { bookmark ->
                    bookmarkedArticles[bookmark.url] = true
                }
            }
        }
    }

    fun isArticleBookmarked(url: String): Boolean {
        return bookmarkedArticles[url] ?: false
    }

    fun toggleBookmark(article: NewsArticle) {
        viewModelScope.launch {
            val url = article.url ?: return@launch
            val isCurrentlyBookmarked = isArticleBookmarked(url)
            
            if (isCurrentlyBookmarked) {
                bookmarkDao.deleteBookmark(Bookmark.fromNewsArticle(article))
                bookmarkedArticles[url] = false
            } else {
                bookmarkDao.insertBookmark(Bookmark.fromNewsArticle(article))
                bookmarkedArticles[url] = true
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(BookmarkViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return BookmarkViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
} 