package com.example.finhub.admin

import android.content.Context
import android.util.Log
import com.example.finhub.data.api.ApiClient
import com.example.finhub.data.database.FirebaseService
import com.example.finhub.data.model.NewsArticle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar

class NewsDataCollector(
    private val finnhubApiKey: String,
    private val newsApiKey: String,
    private val GnewsApiKey: String,
    private val serpApiKey: String,
    private val context: Context
) {
    private val finnhubService = ApiClient.finnhubService
    private val newsApiService = ApiClient.newsApi
    private val gNewsApiService = ApiClient.gNewsApi
    private val serpApiService = ApiClient.serpApi
    private val firebaseService = FirebaseService(context)
    private val processedUrls = Collections.synchronizedSet(HashSet<String>())
    private var progressCallback: ((module: String, current: Int, total: Int, articleTitle: String) -> Unit)? = null
    
    val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date())

    fun setProgressCallback(callback: (module: String, current: Int, total: Int, articleTitle: String) -> Unit) {
        progressCallback = callback
    }

    private suspend fun initializeProcessedUrls() = coroutineScope {
        // Clear processedUrls
        processedUrls.clear()
        // Load processed URLs from Firestore
        FirebaseFirestore.getInstance().collection("articles")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.getString("url")?.let { url ->
                        processedUrls.add(url)
                    }
                }
                Log.d("NewsDataCollector", "Loaded ${processedUrls.size} URLs from Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("NewsDataCollector", "Error loading URLs from Firestore: ${e.message}")
            }
    }

    suspend fun collectAndStoreNewsForModule(moduleIndex: Int) = coroutineScope {
        initializeProcessedUrls()
        Log.d("NewsDataCollector", "Running module $moduleIndex")
        
        when(moduleIndex) {
            0 -> runModule1_SerpDaily()
            1 -> runModule2_GNews()
            2 -> runModule3_NewsApiBusiness()
            3 -> runModule4_NewsApiCryptocurrency()
            4 -> runModule5_NewsApiMutualFunds()
            5 -> runModule6_NewsApiTaxBudgeting()
            6 -> runModule7_NewsApiBankingInsurance()
            7 -> runModule8_NewsApiFintech()
            8 -> runModule9_NewsApiGlobalEconomy()
            9 -> runModule10_SerpBusiness()
            10 -> runModule11_SerpFinance()
            11 -> runModule12_SerpStockMarket()
            12 -> runModule13_SerpTaxBudgeting()
            13 -> runModule14_SerpTechBusiness()
            14 -> runModule15_SerpGlobalEconomy()
            15 -> runModule16_SerpStartups()
            16 -> runModule17_SerpSMEsMSMEs()
            17 -> runModule18_SerpGovernmentPolicies()
            18 -> runModule19_FinnhubApi()
            else -> Log.e("NewsDataCollector", "Invalid module index: $moduleIndex")
        }
    }
    
    // Individual module methods
    private suspend fun runModule1_SerpDaily() {
        try {
            progressCallback?.invoke("SERP Daily", 0, 1, "Starting SERP Daily Finance fetch...")

            val serpNewsFinance = fetchSerpNews("today indian finance", "daily finance", "india")
            val serpNewsBusiness = fetchSerpNews("today indian business", "daily business", "india")

            // Create a calendar instance for date manipulation
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val collectedFinanceArticles = mutableListOf<NewsArticle>()
            val collectedBusinessArticles = mutableListOf<NewsArticle>()

            // Try for today and yesterday only
            for (daysBack in 0..1) {
                val currentDate = dateFormat.format(calendar.time)
                Log.d("NewsDataCollector", "Checking articles for date: $currentDate")

                // Filter and collect finance articles
                if (collectedFinanceArticles.size < 15) {
                    val financeArticlesForDay = serpNewsFinance.filter { article ->
                        article.datetime == currentDate
                    }
                    Log.d("NewsDataCollector", "Found ${financeArticlesForDay.size} finance articles for $currentDate")

                    val remainingFinanceNeeded = 15 - collectedFinanceArticles.size
                    collectedFinanceArticles.addAll(financeArticlesForDay.take(remainingFinanceNeeded))
                }

                // Filter and collect business articles
                if (collectedBusinessArticles.size < 15) {
                    val businessArticlesForDay = serpNewsBusiness.filter { article ->
                        article.datetime == currentDate
                    }
                    Log.d("NewsDataCollector", "Found ${businessArticlesForDay.size} business articles for $currentDate")

                    val remainingBusinessNeeded = 15 - collectedBusinessArticles.size
                    collectedBusinessArticles.addAll(businessArticlesForDay.take(remainingBusinessNeeded))
                }

                // If both categories have 15 articles, we can break early
                if (collectedFinanceArticles.size >= 15 && collectedBusinessArticles.size >= 15) {
                    break
                }

                // Move to previous day
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            val totalArticles = collectedFinanceArticles + collectedBusinessArticles
            Log.d("NewsDataCollector", "Total collected articles: ${totalArticles.size} (Finance: ${collectedFinanceArticles.size}, Business: ${collectedBusinessArticles.size})")

            // Process all articles together
            totalArticles.forEachIndexed { index, article ->
                try {
                    val category = if (article in collectedFinanceArticles) "Finance" else "Business"
                    progressCallback?.invoke("SERP Daily", index + 1, totalArticles.size, "$category: ${article.headline}")

                    article.url?.let { url ->
                        if (url.isNotEmpty() && !processedUrls.contains(url)) {
                            processedUrls.add(url)
                            val docId = firebaseService.storeArticle(article)
                            if (docId != null) {
                                Log.d("NewsDataCollector", "Successfully stored $category article: ${article.headline}")
                            } else {
                                Log.e("NewsDataCollector", "Failed to store $category article: ${article.headline}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Daily module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule2_GNews() {
        try {
            // Signal start of GNews module
            progressCallback?.invoke("GNews", 0, 1, "Starting GNews fetch...")

            val allNews = fetchGNewsBusinessIndia("today finance","daily finance","india") +
                         fetchGNewsBusinessIndia("today business","daily business", "india")
            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles from GNews")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique GNews articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for GNews module
                    progressCallback?.invoke("GNews", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in GNews module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule3_NewsApiBusiness() {
        try {
            progressCallback?.invoke("NewsApi", 0, 1, "Starting NEWS API Business fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("business", "india", "business", true)

            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for NewsApi module
                    progressCallback?.invoke("NewsApi", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in NewsApi Business module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule4_NewsApiCryptocurrency() {
        try {
            progressCallback?.invoke("NewsApi", 0, 1, "Starting NEWS API Cryptocurrency fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("cryptocurrency", "india", "cryptocurrency", false)

            processNewsApiModule(allNews, "NewsApi Cryptocurrency")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in NewsApi Cryptocurrency module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule5_NewsApiMutualFunds() {
        try {
            progressCallback?.invoke("NewsApi", 0, 1, "Starting NEWS API Mutual Funds fetch...")
            val allNews = fetchNewsApiBusinessNews("indian mutual funds", "india", "mutual funds", false)
            processNewsApiModule(allNews, "NewsApi Mutual Funds")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in NewsApi Mutual Funds module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule6_NewsApiTaxBudgeting() {
        try {
            progressCallback?.invoke("NewsApi", 0, 1, "Starting NEWS API Tax & Budgeting fetch...")
            val allNews = fetchNewsApiBusinessNews("indian tax and budgeting", "india", "tax and budgeting", false)
            processNewsApiModule(allNews, "NewsApi Tax & Budgeting")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in NewsApi Tax & Budgeting module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule7_NewsApiBankingInsurance() {
        try {
            progressCallback?.invoke("NewsApi", 0, 1, "Starting NEWS API Banking & Insurance fetch...")
            val allNews = fetchNewsApiBusinessNews("indian banking and insurance", "india", "banking and insurance", false)
            processNewsApiModule(allNews, "NewsApi Banking & Insurance")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in NewsApi Banking & Insurance module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule8_NewsApiFintech() {
        try {
            progressCallback?.invoke("NewsApi", 0, 1, "Starting NEWS API Fintech fetch...")
            val allNews = fetchNewsApiBusinessNews("indian fintech", "india", "fintech", false)
            processNewsApiModule(allNews, "NewsApi Fintech")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in NewsApi Fintech module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule9_NewsApiGlobalEconomy() {
        try {
            progressCallback?.invoke("NewsApi", 0, 1, "Starting NEWS API Global Economy fetch...")
            val allNews = fetchNewsApiBusinessNews("global economy", "india", "global economy", false)
            processNewsApiModule(allNews, "NewsApi Global Economy")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in NewsApi Global Economy module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule10_SerpBusiness() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP Business fetch...")
            val serpNews = fetchSerpNews("business", "business", "india")
            processSerpModule(serpNews, "SERP Business")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Business module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule11_SerpFinance() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP Finance fetch...")
            val serpNews = fetchSerpNews("finance", "finance", "india")
            processSerpModule(serpNews, "SERP Finance")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Finance module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule12_SerpStockMarket() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP Stock Market fetch...")
            val serpNews = fetchSerpNews("stock market", "stock market", "india")
            processSerpModule(serpNews, "SERP Stock Market")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Stock Market module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule13_SerpTaxBudgeting() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP Tax & Budgeting fetch...")
            val serpNews = fetchSerpNews("tax and budgeting", "tax and budgeting", "india")
            processSerpModule(serpNews, "SERP Tax & Budgeting")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Tax & Budgeting module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule14_SerpTechBusiness() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP Tech & Business fetch...")
            val serpNews = fetchSerpNews("tech and business", "tech and business", "india")
            processSerpModule(serpNews, "SERP Tech & Business")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Tech & Business module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule15_SerpGlobalEconomy() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP Global Economy fetch...")
            val serpNews = fetchSerpNews("global economy", "global economy", "india")
            processSerpModule(serpNews, "SERP Global Economy")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Global Economy module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule16_SerpStartups() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP Startups fetch...")
            val serpNews = fetchSerpNews("startups", "startups", "india")
            processSerpModule(serpNews, "SERP Startups")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Startups module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule17_SerpSMEsMSMEs() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP SMEs & MSMEs fetch...")
            val serpNews = fetchSerpNews("SMEs and MSMEs", "SMEs and MSMEs", "india")
            processSerpModule(serpNews, "SERP SMEs & MSMEs")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP SMEs & MSMEs module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule18_SerpGovernmentPolicies() {
        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting SERP Government Policies fetch...")
            val serpNews = fetchSerpNews("government policies", "government policies", "india")
            processSerpModule(serpNews, "SERP Government Policies")
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Government Policies module: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun runModule19_FinnhubApi() {
        try {
            progressCallback?.invoke("Finnhub", 0, 1, "Starting Finnhub API fetch...")
            
            // Get current date in required format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            
            // Fetch finance news from Finnhub API
            val finnhubNews = fetchFinnhubFinanceNews("general", "global")
            Log.d("NewsDataCollector", "Fetched ${finnhubNews.size} articles from Finnhub API")
            
            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            finnhubNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }
            
            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique Finnhub articles (limit: 30)")
            
            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for Finnhub module
                    progressCallback?.invoke("Finnhub", index + 1, articlesToProcess.size, article.headline)
                    
                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in Finnhub module: ${e.message}")
            e.printStackTrace()
        }
    }

    // Helper method to process NewsAPI articles
    private suspend fun processNewsApiModule(allNews: List<NewsArticle>, moduleName: String) {
        Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles from $moduleName")

        val uniqueArticles = mutableListOf<NewsArticle>()
        // Filter out duplicates
        allNews.forEach { article ->
            article.url?.let { url ->
                if (url.isNotEmpty() && !processedUrls.contains(url)) {
                    processedUrls.add(url)
                    uniqueArticles.add(article)
                } else {
                    Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                }
            }
        }

        // Limit to processing 30 articles max
        val articlesToProcess = uniqueArticles.take(30)
        Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

        // Process each unique article sequentially (max 30)
        articlesToProcess.forEachIndexed { index, article ->
            try {
                // Update progress for module
                progressCallback?.invoke(moduleName, index + 1, articlesToProcess.size, article.headline)

                Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                val docId = firebaseService.storeArticle(article)
                if (docId != null) {
                    Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                } else {
                    Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                }
            } catch (e: Exception) {
                Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
            }
        }
    }

    // Helper method to process SERP articles
    private suspend fun processSerpModule(serpNews: List<NewsArticle>, moduleName: String) {
        Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from $moduleName")

        val uniqueArticles = mutableListOf<NewsArticle>()
        // Filter out duplicates
        serpNews.forEach { article ->
            article.url?.let { url ->
                if (url.isNotEmpty() && !processedUrls.contains(url)) {
                    processedUrls.add(url)
                    uniqueArticles.add(article)
                } else {
                    Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                }
            }
        }

        // Limit to processing 30 articles max
        val articlesToProcess = uniqueArticles.take(30)
        Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

        // Process each unique article sequentially (max 30)
        articlesToProcess.forEachIndexed { index, article ->
            try {
                // Update progress for module
                progressCallback?.invoke(moduleName, index + 1, articlesToProcess.size, article.headline)

                Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                val docId = firebaseService.storeArticle(article)
                if (docId != null) {
                    Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                } else {
                    Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                }
            } catch (e: Exception) {
                Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
            }
        }
    }
    
    // Original method that runs all modules (kept for backward compatibility)
    suspend fun collectAndStoreNews() = coroutineScope {
        initializeProcessedUrls()
        Log.d("NewsDataCollector", "Loaded ${processedUrls.size} URLs from Firestore")

        //Module 1 Today SerpApi News
        try {
            progressCallback?.invoke("SERP Daily", 0, 1, "Starting SERP Daily Finance fetch...")

            val serpNewsFinance = fetchSerpNews("today indian finance", "daily finance", "india")
            val serpNewsBusiness = fetchSerpNews("today indian business", "daily business", "india")

            // Create a calendar instance for date manipulation
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val collectedFinanceArticles = mutableListOf<NewsArticle>()
            val collectedBusinessArticles = mutableListOf<NewsArticle>()

            // Try for today and yesterday only
            for (daysBack in 0..1) {
                val currentDate = dateFormat.format(calendar.time)
                Log.d("NewsDataCollector", "Checking articles for date: $currentDate")

                // Filter and collect finance articles
                if (collectedFinanceArticles.size < 15) {
                    val financeArticlesForDay = serpNewsFinance.filter { article ->
                        article.datetime == currentDate
                    }
                    Log.d("NewsDataCollector", "Found ${financeArticlesForDay.size} finance articles for $currentDate")

                    val remainingFinanceNeeded = 15 - collectedFinanceArticles.size
                    collectedFinanceArticles.addAll(financeArticlesForDay.take(remainingFinanceNeeded))
                }

                // Filter and collect business articles
                if (collectedBusinessArticles.size < 15) {
                    val businessArticlesForDay = serpNewsBusiness.filter { article ->
                        article.datetime == currentDate
                    }
                    Log.d("NewsDataCollector", "Found ${businessArticlesForDay.size} business articles for $currentDate")

                    val remainingBusinessNeeded = 15 - collectedBusinessArticles.size
                    collectedBusinessArticles.addAll(businessArticlesForDay.take(remainingBusinessNeeded))
                }

                // If both categories have 15 articles, we can break early
                if (collectedFinanceArticles.size >= 15 && collectedBusinessArticles.size >= 15) {
                    break
                }

                // Move to previous day
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            val totalArticles = collectedFinanceArticles + collectedBusinessArticles
            Log.d("NewsDataCollector", "Total collected articles: ${totalArticles.size} (Finance: ${collectedFinanceArticles.size}, Business: ${collectedBusinessArticles.size})")

            // Process all articles together
            totalArticles.forEachIndexed { index, article ->
                try {
                    val category = if (article in collectedFinanceArticles) "Finance" else "Business"
                    progressCallback?.invoke("SERP Daily", index + 1, totalArticles.size, "$category: ${article.headline}")

                    article.url?.let { url ->
                        if (url.isNotEmpty() && !processedUrls.contains(url)) {
                            processedUrls.add(url)
                            val docId = firebaseService.storeArticle(article)
                            if (docId != null) {
                                Log.d("NewsDataCollector", "Successfully stored $category article: ${article.headline}")
                            } else {
                                Log.e("NewsDataCollector", "Failed to store $category article: ${article.headline}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP Daily module: ${e.message}")
            e.printStackTrace()
        }

        //Module 2 Today Gnews
        try {

            // Signal start of GNews module
            progressCallback?.invoke("GNews", 0, 1, "Starting GNews fetch...")

            val allNews = fetchGNewsBusinessIndia("today finance","daily finance","india") +
                         fetchGNewsBusinessIndia("today business","daily business", "india")
            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles from GNews")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique GNews articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for GNews module
                    progressCallback?.invoke("GNews", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in GNews module: ${e.message}")
            e.printStackTrace()
        }

        //Module 3 NewsApi Business

        try {
            progressCallback?.invoke("NewsApi", 0, 1, "Starting NEWS API fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("business", "india", "business", true)

            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("NewsApi", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
            e.printStackTrace()
        }

        //Module 4 NewsApi  Cryptocurrency

        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting NEWS API fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("cryptocurrency", "india", "cryptocurrency", false)

            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
            e.printStackTrace()
        }

        //Module 5 NewsApi  Mutual funds

        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting NEWS API fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("indian mutual funds", "india", "mutual funds", false)

            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
            e.printStackTrace()
        }

        //Module 6 NewsApi  Tax and Budgeting

        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting NEWS API fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("indian tax and budgeting", "india", "tax and budgeting", false)

            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
            e.printStackTrace()
        }

        //Module 7 NewsApi  banking and insurance

        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting NEWS API fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("indian banking and insurance", "india", "banking and insurance", false)

            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
            e.printStackTrace()
        }

        //Module NewsApi 8 fintech

        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting NEWS API fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("indian fintech", "india", "fintech", false)

            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
            e.printStackTrace()
        }

        //Module 9 NewsApi  global economy

        try {
            progressCallback?.invoke("SERP", 0, 1, "Starting NEWS API fetch...")
            // First fetch all news without processing
            val allNews = fetchNewsApiBusinessNews("global economy", "india", "global economy", false)

            Log.d("NewsDataCollector", "Fetched ${allNews.size} total articles")

            val uniqueArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            allNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueArticles.size} unique articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in collectAndStoreNews: ${e.message}")
            e.printStackTrace()
        }

        // Module 10 SerpApi business

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("business", "business", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // Module 11 SerpApi finance

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("finance", "finance", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // Module 12 SerpApi stock market

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("stock market", "stock market", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // Module 13 SerpApi tax and budgeting

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("tax and budgeting", "tax and budgeting", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // Module 14 SerpApi tech and business

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("tech and business", "tech and business", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // Module 15 SerpApi global economy

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("global economy", "global economy", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // Module 16 SerpApi startups

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("startups", "startups", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // Module 17 SerpApi SMEs and MSMEs

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("SMEs and MSMEs", "SMEs and MSMEs", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // Module 18 SerpApi Government policies

        try {
            progressCallback?.invoke("NEWS", 0, 1, "Starting SERP API fetch...")
            val serpNews = fetchSerpNews("government policies", "government policies", "india")
            Log.d("NewsDataCollector", "Fetched ${serpNews.size} total articles from SERP")

            val uniqueSerpArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            serpNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueSerpArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }

            // Limit to processing 30 articles max
            val articlesToProcess = uniqueSerpArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueSerpArticles.size} unique SERP articles (limit: 30)")

            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for SERP module
                    progressCallback?.invoke("SERP", index + 1, articlesToProcess.size, article.headline)

                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in SERP module: ${e.message}")
            e.printStackTrace()
        }

        // MOdule 19 Finnhub Api
        try {
            progressCallback?.invoke("Finnhub", 0, 1, "Starting Finnhub API fetch...")
            
            // Fetch finance news from Finnhub
            val finnhubNews = fetchFinnhubFinanceNews()
            Log.d("NewsDataCollector", "Fetched ${finnhubNews.size} total articles from Finnhub")
            
            val uniqueFinnhubArticles = mutableListOf<NewsArticle>()
            // Filter out duplicates
            finnhubNews.forEach { article ->
                article.url?.let { url ->
                    if (url.isNotEmpty() && !processedUrls.contains(url)) {
                        processedUrls.add(url)
                        uniqueFinnhubArticles.add(article)
                    } else {
                        Log.d("NewsDataCollector", "Filtered duplicate URL before processing: $url")
                    }
                }
            }
            
            // Limit to processing 30 articles max
            val articlesToProcess = uniqueFinnhubArticles.take(30)
            Log.d("NewsDataCollector", "Processing ${articlesToProcess.size} out of ${uniqueFinnhubArticles.size} unique Finnhub articles (limit: 30)")
            
            // Process each unique article sequentially (max 30)
            articlesToProcess.forEachIndexed { index, article ->
                try {
                    // Update progress for Finnhub module
                    progressCallback?.invoke("Finnhub", index + 1, articlesToProcess.size, article.headline)
                    
                    Log.d("NewsDataCollector", "Attempting to store article: ${article.headline}")
                    val docId = firebaseService.storeArticle(article)
                    if (docId != null) {
                        Log.d("NewsDataCollector", "Successfully stored article: ${article.headline}, ID: $docId")
                    } else {
                        Log.e("NewsDataCollector", "Failed to store article: ${article.headline} (AI processing failed)")
                    }
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error processing article ${article.headline}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error in Finnhub module: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun formatUnixTimestamp(timestamp: String): String {
        return try {
            val timestampLong = timestamp.toLong()
            val date = Date(timestampLong * 1000) // Convert seconds to milliseconds
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error parsing Unix timestamp '$timestamp': ${e.message}")
            ""
        }
    }

    private suspend fun fetchFinnhubFinanceNews(
        category: String = "finance",
        region: String = "global"
    ): List<NewsArticle> {
        return try {
            finnhubService.getBusinessNews(category = category, token = finnhubApiKey)
                .map { article ->
                    NewsArticle(
                        headline = article.headline,
                        image = article.image,
                        source = article.source,
                        datetime = formatUnixTimestamp(article.datetime),
                        summary = article.summary,
                        url = article.url,
                        category = category,
                        region = region,
                        savedDate = currentDate
                    )
                }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error fetching Finnhub news: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchNewsApiBusinessNews(search: String, re: String, cat: String, bus: Boolean): List<NewsArticle> {
        return try {
            val response = if (bus) {
                newsApiService.getBusinessNews(category = search, apiKey = newsApiKey)
            } else {
                // No need to replace anything, use the query as is
                Log.d("NewsDataCollector", "Making NewsAPI request with query: $search")
                newsApiService.getCustomNews(query = search, apiKey = newsApiKey)
            }

            Log.d("NewsDataCollector", "NewsAPI.org Response: $response")
            
            if (response.articles.isEmpty()) {
                Log.w("NewsDataCollector", "No articles found for query: $search")
                return emptyList()
            }

            response.articles.map { article ->
                NewsArticle(
                    headline = article.title,
                    image = article.urlToImage ?: "",
                    source = article.source.name,
                    datetime = formatToDateOnly(article.publishedAt),
                    summary = article.description ?: "",
                    url = article.url,
                    category = cat,
                    region = re,
                    savedDate = currentDate
                )
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error fetching NewsAPI.org news for query '$search': ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun fetchGNewsBusinessIndia(query: String, cat: String, country: String): List<NewsArticle> {
        return try {
            val response = gNewsApiService.searchNews(
                query = query,
                country = country,
                lang = "en",
                token = GnewsApiKey
            )
            Log.d("NewsDataCollector", "GNews Response: $response")
            response.articles.map { article ->
                NewsArticle(
                    headline = article.title,
                    image = article.image ?: "",
                    source = article.source.name,
                    datetime = formatToDateOnly(article.publishedAt),
                    summary = article.description ?: "",
                    url = article.url,
                    category = cat,  // Using query as category
                    region = country,
                    savedDate = currentDate
                )
            }
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error fetching GNews India business: ${e.message}")
            emptyList()
        }
    }

    private fun formatToDateOnly(isoDateTime: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoDateTime ?: "")
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            outputFormat.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatSerpDate(serpDateTime: String?): String {
        return try {
            if (serpDateTime == null) return ""

            // Remove " UTC" (with leading space) so we're left with "+0000"
            val cleanedDate = serpDateTime.replace(" UTC", "").trim()

            val inputFormat = SimpleDateFormat("MM/dd/yyyy, hh:mm a, Z", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val date = inputFormat.parse(cleanedDate)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            outputFormat.format(date!!)
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error parsing SerpAPI date '$serpDateTime': ${e.message}")
            ""
        }
    }

    private suspend fun fetchSerpNews(query: String, cat: String, re: String): List<NewsArticle> {
        return try {
            val response = serpApiService.getGoogleNews(query = query, country = "in",language = "en", apiKey = serpApiKey)
            
            val filteredArticles = response.news_results
            
            Log.d("NewsDataCollector", "After filtering: ${filteredArticles.size} valid articles")
            
            val mappedArticles = filteredArticles.map { article ->
                try {
                    val newsArticle = NewsArticle(
                        headline = article.title,
                        image = article.thumbnail ?: "",
                        source = article.source?.name ?: "Unknown Source",
                        datetime = formatSerpDate(article.date),
                        summary = article.snippet ?: "No summary available",
                        url = article.link,
                        category = cat,
                        region = re,
                        savedDate = currentDate
                    )
                    Log.d("NewsDataCollector", "Successfully mapped article: ${newsArticle.headline}")
                    newsArticle
                } catch (e: Exception) {
                    Log.e("NewsDataCollector", "Error mapping article ${article.title}: ${e.message}")
                    null
                }
            }.filterNotNull()
            
            Log.d("NewsDataCollector", "Final mapped articles: ${mappedArticles.size}")
            mappedArticles
        } catch (e: Exception) {
            Log.e("NewsDataCollector", "Error fetching SerpAPI news for query '$query': ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}

