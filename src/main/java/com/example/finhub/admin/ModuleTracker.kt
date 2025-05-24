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
            0 -> "1. Daily Finance & Business - SERP API "
            1 -> "2. Today's Finance & Business - Gnews API"
            2 -> "3. Business - NewsAPI"
            3 -> "4. Cryptocurrency - NewsAPI"
            4 -> "5. Mutual Funds - NewsAPI"
            5 -> "6. Tax and Budgeting - NewsAPI"
            6 -> "7. Banking and Insurance - NewsAPI"
            7 -> "8. Fintech - NewsAPI"
            8 -> "9. Global Economy - NewsAPI"
            9 -> "10. Business - SERP API"
            10 -> "11. Finance - SERP API"
            11 -> "12. Stock Market - SERP API"
            12 -> "13. Tax and Budgeting - SERP API"
            13 -> "14. Tech and Business - SERP API"
            14 -> "15. Global Economy - SERP API"
            15 -> "16. Startups - SERP API"
            16 -> "17. SMEs and MSMEs - SERP API"
            17 -> "18. Government Policies - SERP API"
            18 -> "19. Finance - Finnhub API"
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
