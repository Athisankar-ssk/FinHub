package com.example.finhub.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.finhub.data.model.NewsArticle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import com.example.finhub.ui.theme.HomeBackgroundTheme
import com.example.finhub.ui.theme.MediumVilot
import com.example.finhub.ui.theme.OnboardingTextSecondary
import java.time.format.DateTimeFormatter
import java.time.*
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ArticleDetailScreen(article: NewsArticle) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeBackgroundTheme)
                .verticalScroll(rememberScrollState())
        ) {
            // Image at the top
            if (!article.image.isNullOrEmpty()) {
                AsyncImage(
                    model = article.image,
                    contentDescription = article.headline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Row for date/time and source
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DevBytesTheme.Purple40)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {


//                    val formattedTime = try {
//                        val input = article.datetime
//                        val zonedDateTime: ZonedDateTime? = when {
//                            input?.matches(Regex("^\\d{10,}$")) == true -> {
//                                // Unix timestamp in seconds
//                                Instant.ofEpochSecond(input.toLong()).atZone(ZoneId.of("Asia/Kolkata"))
//                            }
//                            input != null -> {
//                                // ISO 8601 date string
//                                Instant.parse(input).atZone(ZoneId.of("Asia/Kolkata"))
//                            }
//                            else -> null
//                        }
//
//                        zonedDateTime?.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) ?: "Unknown date"
//                    } catch (e: Exception) {
//                        "Unknown date"
//                    }
//

                    val formattedTime = try {
                        val input = article.datetime // example: "2025-05-09"
                        val date = LocalDate.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val today = LocalDate.now()
                        val yesterday = today.minusDays(1)
                        when (date) {
                            today -> "Today"
                            yesterday -> "Yesterday"
                            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
                        }
                    } catch (e: Exception) {
                        "\uD83D\uDCC6"
                    }


                    Text(
                        text = formattedTime,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DevBytesTheme.Purple40)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = article.source ?: "Unknown source",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Headline
            Text(
                text = article.headline,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                color = Color.White,
                modifier = Modifier
                    .padding(16.dp)
            )

            // Full content
            Text(
                text = article.content ?: "No content available",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = Color.LightGray,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            )

            val context = LocalContext.current
            if (!article.url.isNullOrEmpty()) {
                Text(
                    text ="Source : ${article.source}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp,
                    color = OnboardingTextSecondary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp)
                )

                Text(
                    text = "Read on Publisher's Site ↗",
                    color = MediumVilot,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 46.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                            context.startActivity(intent)
                        }
                )
            }
        }
    }
}
