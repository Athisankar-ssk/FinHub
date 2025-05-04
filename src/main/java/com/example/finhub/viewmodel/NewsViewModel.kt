package com.example.finhub.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finhub.data.database.FirebaseService
import com.example.finhub.data.model.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class NewsViewModel(
    private val context: Context
) : ViewModel() {

    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles

    private val firebaseService = FirebaseService(context)

    init {
        loadArticlesFromFirestore()
    }

    private fun loadArticlesFromFirestore() {
        viewModelScope.launch {
            val articles = firebaseService.getArticles()
            _newsArticles.value = articles
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return NewsViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}