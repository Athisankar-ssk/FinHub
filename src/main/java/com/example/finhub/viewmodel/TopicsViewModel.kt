package com.example.finhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finhub.data.database.FirebaseUserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TopicsViewModel : ViewModel() {
    private val _followedTopics = MutableStateFlow<List<String>>(emptyList())
    val followedTopics: StateFlow<List<String>> = _followedTopics.asStateFlow()

    init {
        loadFollowedTopics()
    }

    private fun loadFollowedTopics() {
        viewModelScope.launch {
            try {
                val topics = FirebaseUserService.getUserInterests()
                _followedTopics.value = topics
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun refreshTopics() {
        loadFollowedTopics()
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TopicsViewModel() as T
            }
        }
    }
} 