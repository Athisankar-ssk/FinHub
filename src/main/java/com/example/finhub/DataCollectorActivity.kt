package com.example.finhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.finhub.admin.AdminScreen
import com.example.finhub.ui.theme.FinHubTheme

class DataCollectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinHubTheme {
                AdminScreenContent()
            }
        }
    }
}

@Composable
fun AdminScreenContent() {
    val navController = rememberNavController()
    AdminScreen(navController = navController)
} 