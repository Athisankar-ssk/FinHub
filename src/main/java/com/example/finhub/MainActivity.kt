package com.example.finhub

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.finhub.ui.navigation.AppNavHost
import com.example.finhub.ui.theme.FinHubTheme
import com.example.finhub.utils.NotificationManager

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shouldOpenAdminNews = intent.getBooleanExtra("openAdminNews", false)
        val adminTab = intent.getStringExtra("adminTab") ?: ""
        
        setContent {
            FinHubTheme {
                FinHubApp(shouldOpenAdminNews = shouldOpenAdminNews, adminTab = adminTab)
            }

        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FinHubApp(shouldOpenAdminNews: Boolean = false, adminTab: String = "") {
    // Main content
    AppNavHost(shouldOpenAdminNews = shouldOpenAdminNews, adminTab = adminTab)
    
    // Notification host that will display notifications from anywhere in the app
    NotificationManager.NotificationHost()
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun FinHubAppPreview() {
    FinHubTheme {
        FinHubApp()
    }

}