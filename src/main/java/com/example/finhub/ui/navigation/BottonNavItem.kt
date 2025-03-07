package com.example.finhub.ui.navigation

import com.example.finhub.R

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: Int
) {
    object Home : BottomNavItem("home", "Home", R.drawable.home_icon)
    object Trending : BottomNavItem("trending", "Trending", R.drawable.trending_icon)
    object Markets : BottomNavItem("markets", "Markets", R.drawable.market_icon)
    object Bookmark : BottomNavItem("bookmark", "Bookmark", R.drawable.bookmark_icon)
}
