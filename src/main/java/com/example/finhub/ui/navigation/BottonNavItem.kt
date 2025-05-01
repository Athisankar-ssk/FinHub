package com.example.finhub.ui.navigation

import com.example.finhub.R

sealed class BottomNavItem(val route: String, val icon: Int, val label: String) {
    object Home : BottomNavItem("home", R.drawable.home_icon, "Home")
    object Trending : BottomNavItem("trending", R.drawable.trending_icon, "Trending")
    object Markets : BottomNavItem("markets", R.drawable.market_icon, "Markets")
    object Bookmarks : BottomNavItem("bookmarks", R.drawable.bookmark_icon, "Bookmarks")
}