package com.example.finhub.admin

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finhub.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminNews() {
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
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
            .background(HomeBackgroundTheme)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fetch News Button
        Button(
            onClick = {
                if (!isNetworkAvailable(context)) {
                    Toast.makeText(context, "No internet connection available", Toast.LENGTH_LONG).show()
                    return@Button
                }
                
                scope.launch {
                    isLoading = true
                    statusMessage = "Fetching news..."
                    try {
                        val collector = NewsDataCollector(
                            finnhubApiKey = finnhubApiKey,
                            newsApiKey = newsApiKey,
                            GnewsApiKey = GnewsApiKey,
                            serpApiKey = SerpApiKey,
                            context = context
                        )
                        collector.collectAndStoreNews()
                        statusMessage = "News fetched successfully!"
                        Toast.makeText(context, "News collection completed!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        val errorMessage = when {
                            e.message?.contains("Unable to resolve host") == true -> 
                                "No internet connection. Please check your network settings."
                            else -> "Error: ${e.message}"
                        }
                        statusMessage = errorMessage
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Follow,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Fetch News"
                    )
                    Text(
                        "Fetch Latest News",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Status Message
        statusMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardPurple
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
} 