package com.polaralias.signalsynthesis.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ActivityType { API_REQUEST, LLM_REQUEST }

enum class ApiUsageCategory {
    DISCOVERY,      // Finding tickers (screener, gainers, losers, actives)
    ANALYSIS,       // Reviewing trends (quotes, intraday, daily data)
    FUNDAMENTALS,   // Profile, metrics, sentiment
    ALERTS,         // Background alert checks
    SEARCH,         // Ticker search
    OTHER;          // Fallback

    companion object {
        fun fromOperation(operation: String): ApiUsageCategory {
            return when {
                operation.contains("Screener", ignoreCase = true) -> DISCOVERY
                operation.contains("Gainer", ignoreCase = true) -> DISCOVERY
                operation.contains("Loser", ignoreCase = true) -> DISCOVERY
                operation.contains("Active", ignoreCase = true) -> DISCOVERY
                operation.contains("Quote", ignoreCase = true) -> ANALYSIS
                operation.contains("Intraday", ignoreCase = true) -> ANALYSIS
                operation.contains("Daily", ignoreCase = true) -> ANALYSIS
                operation.contains("Profile", ignoreCase = true) -> FUNDAMENTALS
                operation.contains("Metrics", ignoreCase = true) -> FUNDAMENTALS
                operation.contains("Sentiment", ignoreCase = true) -> FUNDAMENTALS
                operation.contains("Alert", ignoreCase = true) -> ALERTS
                operation.contains("Search", ignoreCase = true) -> SEARCH
                else -> OTHER
            }
        }
    }
}

data class ActivityEntry(
    val timestamp: Instant = Instant.now(),
    val type: ActivityType,
    val tag: String,
    val input: String,
    val output: String,
    val isSuccess: Boolean,
    val durationMs: Long = 0,
    val category: ApiUsageCategory = ApiUsageCategory.OTHER
)

data class DailyUsageArchive(
    val date: LocalDate,
    val totalCalls: Int,
    val providerBreakdown: Map<String, Map<ApiUsageCategory, Int>>
)

object ActivityLogger {
    private const val MAX_ENTRIES = 100
    private val _activities = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val activities: StateFlow<List<ActivityEntry>> = _activities.asStateFlow()

    fun logApi(tag: String, input: String, output: String, isSuccess: Boolean, durationMs: Long) {
        val category = ApiUsageCategory.fromOperation(input)
        addEntry(ActivityEntry(
            type = ActivityType.API_REQUEST, 
            tag = tag, 
            input = input, 
            output = output, 
            isSuccess = isSuccess, 
            durationMs = durationMs,
            category = category
        ))
        // Now log ALL providers including mock to help users understand usage patterns
        if (isSuccess) {
            UsageTracker.incrementApiCount(tag, category)
        }
    }

    fun logLlm(tag: String, input: String, output: String, isSuccess: Boolean, durationMs: Long) {
        addEntry(ActivityEntry(type = ActivityType.LLM_REQUEST, tag = tag, input = input, output = output, isSuccess = isSuccess, durationMs = durationMs))
    }

    private fun addEntry(entry: ActivityEntry) {
        _activities.update { current ->
            (listOf(entry) + current).take(MAX_ENTRIES)
        }
    }

    fun clear() {
        _activities.update { emptyList() }
    }
}

object UsageTracker {
    private const val PREFS_NAME = "usage_prefs"
    private const val ARCHIVE_PREFS_NAME = "usage_archive_prefs"
    private const val KEY_API_COUNT = "api_count_day"
    private const val KEY_PROVIDER_PREFIX = "provider_"
    private const val KEY_CATEGORY_SUFFIX = "_category_"
    private const val KEY_LAST_DAY = "last_tracked_day"
    private const val MAX_ARCHIVE_DAYS = 30

    private var context: Context? = null
    private val _dailyApiCount = MutableStateFlow(0)
    val dailyApiCount: StateFlow<Int> = _dailyApiCount.asStateFlow()

    private val _dailyProviderUsage = MutableStateFlow<Map<String, Map<ApiUsageCategory, Int>>>(emptyMap())
    val dailyProviderUsage: StateFlow<Map<String, Map<ApiUsageCategory, Int>>> = _dailyProviderUsage.asStateFlow()

    private val _archivedUsage = MutableStateFlow<List<DailyUsageArchive>>(emptyList())
    val archivedUsage: StateFlow<List<DailyUsageArchive>> = _archivedUsage.asStateFlow()

    fun init(ctx: Context) {
        context = ctx.applicationContext
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        
        val currentDay = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lastDay = prefs.getString(KEY_LAST_DAY, "")

        if (currentDay != lastDay) {
            // Archive yesterday's data before resetting
            if (lastDay?.isNotBlank() == true) {
                archiveCurrentDay(lastDay)
            }
            
            // Reset for new day
            prefs.edit()
                .clear()
                .putString(KEY_LAST_DAY, currentDay)
                .apply()
            _dailyApiCount.value = 0
            _dailyProviderUsage.value = emptyMap()
        } else {
            loadCurrentDay(prefs)
        }
        
        loadArchive()
    }

    private fun loadCurrentDay(prefs: android.content.SharedPreferences) {
        _dailyApiCount.value = prefs.getInt(KEY_API_COUNT, 0)
        val providerMap = mutableMapOf<String, MutableMap<ApiUsageCategory, Int>>()
        
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_PROVIDER_PREFIX) && value is Int) {
                val parts = key.removePrefix(KEY_PROVIDER_PREFIX).split(KEY_CATEGORY_SUFFIX)
                if (parts.size == 2) {
                    val provider = parts[0]
                    val category = try {
                        ApiUsageCategory.valueOf(parts[1])
                    } catch (e: Exception) {
                        ApiUsageCategory.OTHER
                    }
                    
                    providerMap.getOrPut(provider) { mutableMapOf() }[category] = value
                }
            }
        }
        _dailyProviderUsage.value = providerMap
    }

    private fun archiveCurrentDay(dayString: String) {
        val archivePrefs = context?.getSharedPreferences(ARCHIVE_PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val date = try {
            LocalDate.parse(dayString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            return
        }

        val totalCalls = _dailyApiCount.value
        val providerBreakdown = _dailyProviderUsage.value.toMap()

        // Store in archive prefs
        archivePrefs.edit().apply {
            putInt("${dayString}_total", totalCalls)
            providerBreakdown.forEach { (provider, categories) ->
                categories.forEach { (category, count) ->
                    putInt("${dayString}_${provider}_${category.name}", count)
                }
            }
            apply()
        }

        // Clean up old archives (keep last 30 days)
        val allKeys = archivePrefs.all.keys.toList()
        val datePrefixes = allKeys.mapNotNull { key ->
            val parts = key.split("_")
            if (parts.isNotEmpty()) parts[0] else null
        }.distinct().sorted()

        if (datePrefixes.size > MAX_ARCHIVE_DAYS) {
            val toRemove = datePrefixes.take(datePrefixes.size - MAX_ARCHIVE_DAYS)
            archivePrefs.edit().apply {
                toRemove.forEach { datePrefix ->
                    allKeys.filter { it.startsWith(datePrefix) }.forEach { remove(it) }
                }
                apply()
            }
        }
    }

    private fun loadArchive() {
        val archivePrefs = context?.getSharedPreferences(ARCHIVE_PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val archives = mutableListOf<DailyUsageArchive>()

        val allKeys = archivePrefs.all.keys.toList()
        val datePrefixes = allKeys.mapNotNull { key ->
            val parts = key.split("_")
            if (parts.isNotEmpty() && parts[0].matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) parts[0] else null
        }.distinct().sorted().reversed()

        datePrefixes.forEach { dateString ->
            try {
                val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                val total = archivePrefs.getInt("${dateString}_total", 0)
                
                val providerBreakdown = mutableMapOf<String, MutableMap<ApiUsageCategory, Int>>()
                allKeys.filter { it.startsWith(dateString) && it != "${dateString}_total" }.forEach { key ->
                    val parts = key.removePrefix("${dateString}_").split("_")
                    if (parts.size >= 2) {
                        val provider = parts.dropLast(1).joinToString("_")
                        val category = try {
                            ApiUsageCategory.valueOf(parts.last())
                        } catch (e: Exception) {
                            ApiUsageCategory.OTHER
                        }
                        val count = archivePrefs.getInt(key, 0)
                        providerBreakdown.getOrPut(provider) { mutableMapOf() }[category] = count
                    }
                }

                archives.add(DailyUsageArchive(date, total, providerBreakdown))
            } catch (e: Exception) {
                // Skip invalid entries
            }
        }

        _archivedUsage.value = archives
    }

    fun incrementApiCount(provider: String, category: ApiUsageCategory) {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        
        val totalCount = _dailyApiCount.value + 1
        _dailyApiCount.value = totalCount

        val updatedMap = _dailyProviderUsage.value.toMutableMap()
        val providerCategories = updatedMap.getOrPut(provider) { mutableMapOf() }.toMutableMap()
        providerCategories[category] = (providerCategories[category] ?: 0) + 1
        updatedMap[provider] = providerCategories
        _dailyProviderUsage.value = updatedMap
        
        val categoryKey = "$KEY_PROVIDER_PREFIX${provider}${KEY_CATEGORY_SUFFIX}${category.name}"
        
        prefs.edit().apply {
            putInt(KEY_API_COUNT, totalCount)
            putInt(categoryKey, providerCategories[category]!!)
            apply()
        }
    }

    fun manualArchive() {
        val currentDay = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        archiveCurrentDay(currentDay)
        
        // Reset current day
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        prefs.edit().clear().putString(KEY_LAST_DAY, currentDay).apply()
        _dailyApiCount.value = 0
        _dailyProviderUsage.value = emptyMap()
        
        loadArchive()
    }
}
