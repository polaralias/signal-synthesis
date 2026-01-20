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

data class ActivityEntry(
    val timestamp: Instant = Instant.now(),
    val type: ActivityType,
    val tag: String,
    val input: String,
    val output: String,
    val isSuccess: Boolean,
    val durationMs: Long = 0
)

object ActivityLogger {
    private const val MAX_ENTRIES = 100
    private val _activities = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val activities: StateFlow<List<ActivityEntry>> = _activities.asStateFlow()

    fun logApi(tag: String, input: String, output: String, isSuccess: Boolean, durationMs: Long) {
        addEntry(ActivityEntry(type = ActivityType.API_REQUEST, tag = tag, input = input, output = output, isSuccess = isSuccess, durationMs = durationMs))
        if (isSuccess) {
            UsageTracker.incrementApiCount(tag)
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
    private const val KEY_API_COUNT = "api_count_month"
    private const val KEY_PROVIDER_PREFIX = "provider_count_"
    private const val KEY_LAST_MONTH = "last_tracked_month"

    private var context: Context? = null
    private val _monthlyApiCount = MutableStateFlow(0)
    val monthlyApiCount: StateFlow<Int> = _monthlyApiCount.asStateFlow()

    private val _monthlyProviderUsage = MutableStateFlow<Map<String, Int>>(emptyMap())
    val monthlyProviderUsage: StateFlow<Map<String, Int>> = _monthlyProviderUsage.asStateFlow()

    fun init(ctx: Context) {
        context = ctx.applicationContext
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val lastMonth = prefs.getString(KEY_LAST_MONTH, "")

        if (currentMonth != lastMonth) {
            prefs.edit()
                .clear()
                .putString(KEY_LAST_MONTH, currentMonth)
                .apply()
            _monthlyApiCount.value = 0
            _monthlyProviderUsage.value = emptyMap()
        } else {
            _monthlyApiCount.value = prefs.getInt(KEY_API_COUNT, 0)
            val providerMap = mutableMapOf<String, Int>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(KEY_PROVIDER_PREFIX) && value is Int) {
                    providerMap[key.removePrefix(KEY_PROVIDER_PREFIX)] = value
                }
            }
            _monthlyProviderUsage.value = providerMap
        }
    }

    fun incrementApiCount(provider: String) {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        
        val totalCount = _monthlyApiCount.value + 1
        _monthlyApiCount.value = totalCount

        val providerKey = KEY_PROVIDER_PREFIX + provider
        val providerCount = (_monthlyProviderUsage.value[provider] ?: 0) + 1
        
        val updatedMap = _monthlyProviderUsage.value.toMutableMap()
        updatedMap[provider] = providerCount
        _monthlyProviderUsage.value = updatedMap
        
        prefs.edit().apply {
            putInt(KEY_API_COUNT, totalCount)
            putInt(providerKey, providerCount)
            apply()
        }
    }
}
