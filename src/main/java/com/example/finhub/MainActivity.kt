
package com.example.finhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    val finnhubApiKey = "cuslsapr01qnihs7d1lgcuslsapr01qnihs7d1m0" // Replace with your actual Finnhub API key
    val newsApiKey = "cda48b3dfe61437492014dec4ef5a703" // Replace with your actual NewsAPI.org API key

    FinHubTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(finnhubApiKey = finnhubApiKey, newsApiKey = newsApiKey)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinHubAppPreview() {
    FinHubApp()
}
