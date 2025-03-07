package com.example.finhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finhub.data.model.NewsArticle
import com.example.finhub.data.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsViewModel(private val apiKey: String) : ViewModel() {

    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles

    init {
        fetchNews()
    }

    private fun fetchNews() {
        viewModelScope.launch {
            try {
                val response = ApiClient.finnhubService.getBusinessNews(
                    category = "general",
                    token = apiKey
                )
                _newsArticles.value = response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
