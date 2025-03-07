package com.example.finhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.finhub.ui.navigation.AppNavHost
import com.example.finhub.ui.theme.FinHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinHubApp()
        }
    }
}

@Composable
fun FinHubApp() {
    val apiKey = "cuslsapr01qnihs7d1lgcuslsapr01qnihs7d1m0"  // Replace with your actual key

    FinHubTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(apiKey = apiKey)  // ✅ Pass apiKey into AppNavHost
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinHubAppPreview() {
    FinHubApp()
}
