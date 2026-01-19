package com.polaralias.signalsynthesis.data.repository

import com.polaralias.signalsynthesis.data.cache.TimedCache
import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.data.provider.RetryHelper
import com.polaralias.signalsynthesis.util.Logger

class MarketDataRepository(
    private val providers: ProviderBundle
) {
    private val quoteCache = TimedCache<String, Map<String, Quote>>(QUOTE_TTL_MILLIS)
    private val intradayCache = TimedCache<String, List<IntradayBar>>(INTRADAY_TTL_MILLIS)
    private val dailyCache = TimedCache<String, List<DailyBar>>(DAILY_TTL_MILLIS)
    private val profileCache = TimedCache<String, CompanyProfile>(PROFILE_TTL_MILLIS)
    private val metricsCache = TimedCache<String, FinancialMetrics>(METRICS_TTL_MILLIS)
    private val sentimentCache = TimedCache<String, SentimentData>(SENTIMENT_TTL_MILLIS)

    suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        if (symbols.isEmpty()) return emptyMap()
        val key = quoteKey(symbols)
        quoteCache.get(key)?.let { 
            Logger.d("Repository", "Cache hit for quotes: ${symbols.take(3)}...")
            return it 
        }
        val result = tryProviders(
            dataType = "Quotes",
            providers = providers.quoteProviders,
            fetch = { it.getQuotes(symbols) },
            isValid = { it.isNotEmpty() }
        ) ?: emptyMap()
        if (result.isNotEmpty()) {
            quoteCache.put(key, result)
        }
        return result
    }

    suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        val key = "intraday:$symbol:$days"
        intradayCache.get(key)?.let { 
            Logger.d("Repository", "Cache hit for intraday: $symbol")
            return it 
        }
        val result = tryProviders(
            dataType = "Intraday($symbol)",
            providers = providers.intradayProviders,
            fetch = { it.getIntraday(symbol, days) },
            isValid = { it.isNotEmpty() }
        ) ?: emptyList()
        if (result.isNotEmpty()) {
            intradayCache.put(key, result)
        }
        return result
    }

    suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        val key = "daily:$symbol:$days"
        dailyCache.get(key)?.let { 
            Logger.d("Repository", "Cache hit for daily: $symbol")
            return it 
        }
        val result = tryProviders(
            dataType = "Daily($symbol)",
            providers = providers.dailyProviders,
            fetch = { it.getDaily(symbol, days) },
            isValid = { it.isNotEmpty() }
        ) ?: emptyList()
        if (result.isNotEmpty()) {
            dailyCache.put(key, result)
        }
        return result
    }

    suspend fun getProfile(symbol: String): CompanyProfile? {
        if (symbol.isBlank()) return null
        profileCache.get(symbol)?.let { 
            Logger.d("Repository", "Cache hit for profile: $symbol")
            return it 
        }
        val result = tryProviders(
            dataType = "Profile($symbol)",
            providers = providers.profileProviders,
            fetch = { it.getProfile(symbol) },
            isValid = { it != null }
        )
        if (result != null) {
            profileCache.put(symbol, result)
        }
        return result
    }

    suspend fun getMetrics(symbol: String): FinancialMetrics? {
        if (symbol.isBlank()) return null
        metricsCache.get(symbol)?.let { 
            Logger.d("Repository", "Cache hit for metrics: $symbol")
            return it 
        }
        val result = tryProviders(
            dataType = "Metrics($symbol)",
            providers = providers.metricsProviders,
            fetch = { it.getMetrics(symbol) },
            isValid = { it != null }
        )
        if (result != null) {
            metricsCache.put(symbol, result)
        }
        return result
    }

    suspend fun getSentiment(symbol: String): SentimentData? {
        if (symbol.isBlank()) return null
        sentimentCache.get(symbol)?.let { 
            Logger.d("Repository", "Cache hit for sentiment: $symbol")
            return it 
        }
        val result = tryProviders(
            dataType = "Sentiment($symbol)",
            providers = providers.sentimentProviders,
            fetch = { it.getSentiment(symbol) },
            isValid = { it != null }
        )
        if (result != null) {
            sentimentCache.put(symbol, result)
        }
        return result
    }

    private suspend fun <P : Any, T> tryProviders(
        dataType: String,
        providers: List<P>,
        fetch: suspend (P) -> T,
        isValid: (T) -> Boolean
    ): T? {
        Logger.i("Repository", "Fetching $dataType from ${providers.size} providers")
        for (provider in providers) {
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                val result = RetryHelper.withRetry(providerName) {
                    fetch(provider)
                }
                if (isValid(result)) {
                    Logger.i("Repository", "SUCCESS: $dataType from $providerName")
                    return result
                }
                Logger.w("Repository", "EMPTY: $dataType from $providerName")
            } catch (e: Exception) {
                Logger.w("Repository", "FAILED: $dataType from $providerName", e)
            }
        }
        Logger.e("Repository", "CRITICAL: All providers failed for $dataType")
        return null
    }

    private fun quoteKey(symbols: List<String>): String {
        return "quotes:" + symbols.map { it.trim() }.sorted().joinToString(",")
    }

    companion object {
        private const val QUOTE_TTL_MILLIS = 5_000L
        private const val INTRADAY_TTL_MILLIS = 2 * 60_000L
        private const val DAILY_TTL_MILLIS = 24 * 60 * 60_000L
        private const val PROFILE_TTL_MILLIS = 24 * 60 * 60_000L
        private const val METRICS_TTL_MILLIS = 24 * 60 * 60_000L
        private const val SENTIMENT_TTL_MILLIS = 15 * 60_000L
    }
}
