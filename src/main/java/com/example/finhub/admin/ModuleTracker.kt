package com.example.finhub.admin

import android.content.Context

class ModuleTracker(private val context: Context) {
    private val prefs = context.getSharedPreferences("news_module_tracker", Context.MODE_PRIVATE)
    
    // Get the next module to run (0-18 for your 19 modules)
    fun getNextModuleToRun(): Int {
        val lastRunModule = prefs.getInt("last_run_module", -1)
        val nextModule = (lastRunModule + 1) % 19
        return nextModule
    }
    
    // Mark a module as completed
    fun markModuleCompleted(moduleIndex: Int) {
        prefs.edit().putInt("last_run_module", moduleIndex).apply()
        prefs.edit().putLong("module_${moduleIndex}_last_run", System.currentTimeMillis()).apply()
        
        // Also store which module was last run for user display
        val moduleName = getModuleName(moduleIndex)
        prefs.edit().putString("last_module_name", moduleName).apply()
    }
    
    // Get a user-friendly name for the module
    fun getModuleName(moduleIndex: Int): String {
        return when(moduleIndex) {
            0 -> "SERP Daily Finance & Business"
            1 -> "GNews Today's Finance & Business"
            2 -> "NewsAPI Business"
            3 -> "NewsAPI Cryptocurrency"
            4 -> "NewsAPI Mutual Funds"
            5 -> "NewsAPI Tax and Budgeting"
            6 -> "NewsAPI Banking and Insurance"
            7 -> "NewsAPI Fintech"
            8 -> "NewsAPI Global Economy"
            9 -> "SERP Business"
            10 -> "SERP Finance"
            11 -> "SERP Stock Market"
            12 -> "SERP Tax and Budgeting"
            13 -> "SERP Tech and Business"
            14 -> "SERP Global Economy"
            15 -> "SERP Startups"
            16 -> "SERP SMEs and MSMEs"
            17 -> "SERP Government Policies"
            18 -> "Finnhub Finance"
            else -> "Unknown Module"
        }
    }
    
    // Get the last time a module was run
    fun getLastRunTimeForModule(moduleIndex: Int): Long {
        return prefs.getLong("module_${moduleIndex}_last_run", 0)
    }
    
    // Get the name of the last completed module
    fun getLastCompletedModuleName(): String {
        return prefs.getString("last_module_name", "None") ?: "None"
    }
}
