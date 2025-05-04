package com.example.finhub

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.finhub.data.service.NewsDataCollector
import kotlinx.coroutines.launch

class DataCollectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DataCollectorScreen()
        }
    }
}

@Composable
fun DataCollectorScreen() {
    var isCollecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // API Keys
    val finnhubApiKey = "cuslsapr01qnihs7d1lgcuslsapr01qnihs7d1m0"
    val newsApiKey = "cda48b3dfe61437492014dec4ef5a703"
    val GnewsApiKey = "d3c91fe2dce9b0b8f4fb7302cb4043b6"
    val SerpApiKey = "b6db71dcb10bc63b92dcd1d100d742a63c866b30837a3254d5a61ee84ae8a630"

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                if (!isNetworkAvailable(context)) {
                    Toast.makeText(context, "No internet connection available", Toast.LENGTH_LONG).show()
                    return@Button
                }
                
                isCollecting = true
                scope.launch {
                    try {
                        val collector = NewsDataCollector(
                            finnhubApiKey = finnhubApiKey,
                            newsApiKey = newsApiKey,
                            GnewsApiKey = GnewsApiKey,
                            serpApiKey = SerpApiKey,
                            context = context
                        )
                        collector.collectAndStoreNews()
                        Toast.makeText(context, "News collection completed!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        val errorMessage = when {
                            e.message?.contains("Unable to resolve host") == true -> 
                                "No internet connection. Please check your network settings."
                            else -> "Error: ${e.message}"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    } finally {
                        isCollecting = false
                    }
                }
            },
            enabled = !isCollecting
        ) {
            Text(if (isCollecting) "Collecting..." else "Collect News Data")
        }

        if (isCollecting) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
} 