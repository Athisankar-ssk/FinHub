package com.example.finhub.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

fun RandomFact(): String {
    val facts = listOf(
        "Did you know? The first computer 'bug' was an actual insect stuck in a relay.",
        "Bitcoin's creator, Satoshi Nakamoto, remains anonymous to this day.",
        "The first website ever created is still online at CERN.",
        "Python was named after Monty Python, not the snake.",
        "The term 'byte' was coined in 1956 by Werner Buchholz."
    )
    return facts.random()
}