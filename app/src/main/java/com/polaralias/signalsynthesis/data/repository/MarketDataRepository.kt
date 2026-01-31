package com.polaralias.signalsynthesis.data.repository

import com.polaralias.signalsynthesis.data.cache.TimedCache
import com.polaralias.signalsynthesis.data.cache.CacheTtlConfig
import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.data.provider.ProviderStatusManager
import com.polaralias.signalsynthesis.data.provider.RetryHelper
import com.polaralias.signalsynthesis.domain.provider.SearchResult
import com.polaralias.signalsynthesis.util.Logger
import kotlinx.coroutines.delay

class MarketDataRepository(
    private val providers: ProviderBundle,
    cacheConfig: CacheTtlConfig = CacheTtlConfig()
) {

    suspend fun searchSymbols(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return tryProviders(
            dataType = "Search($query)",
            providers = providers.searchProviders,
            fetch = { it.searchSymbols(query) },
            isValid = { it.isNotEmpty() }
        ) ?: emptyList()
    }
    private val quoteCache = TimedCache<String, Quote>(cacheConfig.quoteTtlMs)
    private val intradayCache = TimedCache<String, List<IntradayBar>>(cacheConfig.intradayTtlMs)
    private val dailyCache = TimedCache<String, List<DailyBar>>(cacheConfig.dailyTtlMs)
    private val profileCache = TimedCache<String, CompanyProfile>(cacheConfig.profileTtlMs)
    private val metricsCache = TimedCache<String, FinancialMetrics>(cacheConfig.metricsTtlMs)
    private val sentimentCache = TimedCache<String, SentimentData>(cacheConfig.sentimentTtlMs)

    suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        if (symbols.isEmpty()) return emptyMap()
        
        val result = mutableMapOf<String, Quote>()
        val missing = symbols.map { it.trim() }.toMutableList()
        
        // 1. Try Cache
        val iterator = missing.iterator()
        while (iterator.hasNext()) {
            val symbol = iterator.next()
            quoteCache.get(symbol)?.let {
                result[symbol] = it
                iterator.remove()
            }
        }
        
        if (missing.isEmpty()) {
            Logger.d("Repository", "Full cache hit for quotes: ${symbols.take(3)}...")
            return result
        }

        Logger.d("Repository", "Fetching ${missing.size} missing quotes from providers...")
        
        // 2. Try Providers sequentially for missing symbols
        val filteredProviders = providers.quoteProviders.filter { provider ->
            val providerName = provider::class.simpleName ?: "Unknown"
            !ProviderStatusManager.isBlacklisted(providerName)
        }

        for (provider in filteredProviders) {
            if (missing.isEmpty()) break
            
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                delay(200) // Rate limit prevention
                val fetched = RetryHelper.withRetry(providerName) {
                    provider.getQuotes(missing)
                }
                
                if (fetched.isNotEmpty()) {
                    fetched.forEach { (symbol, quote) ->
                        quoteCache.put(symbol, quote)
                        result[symbol] = quote
                        missing.remove(symbol)
                    }
                    Logger.d("Repository", "Fetched ${fetched.size} quotes from $providerName. ${missing.size} still missing.")
                    com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Quotes(${fetched.size})", "Success", true, 0)
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Quotes", errorMessage, false, 0)
                
                if (errorMessage.contains("403") || (e is retrofit2.HttpException && e.code() == 403)) {
                    ProviderStatusManager.blacklistProvider(providerName, ENFORCED_COOLDOWN_MS)
                }
            }
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

        var aggregatedProfile: CompanyProfile? = null
        val filteredProviders = providers.profileProviders.filter { provider ->
            !ProviderStatusManager.isBlacklisted(provider::class.simpleName ?: "Unknown")
        }

        for (provider in filteredProviders) {
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                delay(200)
                val result = RetryHelper.withRetry(providerName) {
                    provider.getProfile(symbol)
                }
                if (result != null) {
                    if (aggregatedProfile == null) {
                        aggregatedProfile = result
                    } else {
                        aggregatedProfile = aggregatedProfile.copy(
                            name = if (aggregatedProfile.name == symbol || aggregatedProfile.name.isBlank()) result.name else aggregatedProfile.name,
                            sector = aggregatedProfile.sector ?: result.sector,
                            industry = aggregatedProfile.industry ?: result.industry,
                            description = aggregatedProfile.description ?: result.description
                        )
                    }

                    if (aggregatedProfile.sector != null && aggregatedProfile.description != null) {
                        break
                    }
                }
            } catch (e: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Profile", e.message ?: "Error", false, 0)
            }
        }

        if (aggregatedProfile != null) {
            profileCache.put(symbol, aggregatedProfile)
        }
        return aggregatedProfile
    }

    suspend fun getMetrics(symbol: String): FinancialMetrics? {
        if (symbol.isBlank()) return null
        metricsCache.get(symbol)?.let { 
            Logger.d("Repository", "Cache hit for metrics: $symbol")
            return it 
        }

        var aggregatedMetrics: FinancialMetrics? = null
        val filteredProviders = providers.metricsProviders.filter { provider ->
            !ProviderStatusManager.isBlacklisted(provider::class.simpleName ?: "Unknown")
        }

        for (provider in filteredProviders) {
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                delay(200)
                val result = RetryHelper.withRetry(providerName) {
                    provider.getMetrics(symbol)
                }
                if (result != null) {
                    if (aggregatedMetrics == null) {
                        aggregatedMetrics = result
                    } else {
                        // Fill in missing fields
                        aggregatedMetrics = aggregatedMetrics.copy(
                            marketCap = aggregatedMetrics.marketCap ?: result.marketCap,
                            peRatio = aggregatedMetrics.peRatio ?: result.peRatio,
                            eps = aggregatedMetrics.eps ?: result.eps,
                            earningsDate = aggregatedMetrics.earningsDate ?: result.earningsDate,
                            dividendYield = aggregatedMetrics.dividendYield ?: result.dividendYield,
                            pbRatio = aggregatedMetrics.pbRatio ?: result.pbRatio,
                            debtToEquity = aggregatedMetrics.debtToEquity ?: result.debtToEquity
                        )
                    }
                    
                    // If we have the most critical fields, we can stop early
                    if (aggregatedMetrics.marketCap != null && aggregatedMetrics.peRatio != null && aggregatedMetrics.eps != null) {
                        Logger.d("Repository", "Collected all critical metrics for $symbol from $providerName (and potentially others).")
                        break
                    }
                }
            } catch (e: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Metrics", e.message ?: "Error", false, 0)
            }
        }

        if (aggregatedMetrics != null) {
            metricsCache.put(symbol, aggregatedMetrics)
        }
        return aggregatedMetrics
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

    suspend fun screenStocks(
        minPrice: Double?,
        maxPrice: Double?,
        minVolume: Long?,
        sector: String?,
        limit: Int
    ): List<String> {
        val allResults = mutableSetOf<String>()
        val filteredProviders = providers.screenerProviders.filter { provider ->
            !ProviderStatusManager.isBlacklisted(provider::class.simpleName ?: "Unknown")
        }

        for (provider in filteredProviders) {
            if (allResults.size >= limit) break
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                delay(200)
                val results = RetryHelper.withRetry(providerName) {
                    provider.screenStocks(minPrice, maxPrice, minVolume, sector, limit - allResults.size)
                }
                if (results.isNotEmpty()) {
                    allResults.addAll(results)
                    Logger.d("Repository", "Added ${results.size} from $providerName to screener results.")
                    com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Screener", "Success", true, 0)
                }
            } catch (e: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Screener", e.message ?: "Error", false, 0)
            }
        }
        return allResults.toList()
    }

    suspend fun getTopGainers(limit: Int = 10): List<String> {
        val allResults = mutableSetOf<String>()
        val filteredProviders = providers.screenerProviders.filter { provider ->
            !ProviderStatusManager.isBlacklisted(provider::class.simpleName ?: "Unknown")
        }

        for (provider in filteredProviders) {
            if (allResults.size >= limit) break
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                delay(200)
                val results = RetryHelper.withRetry(providerName) {
                    provider.getTopGainers(limit - allResults.size)
                }
                if (results.isNotEmpty()) {
                    allResults.addAll(results)
                    com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Gainers", "Success", true, 0)
                }
            } catch (e: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Gainers", e.message ?: "Error", false, 0)
            }
        }
        return allResults.toList()
    }

    suspend fun getTopLosers(limit: Int = 10): List<String> {
        val allResults = mutableSetOf<String>()
        val filteredProviders = providers.screenerProviders.filter { provider ->
            !ProviderStatusManager.isBlacklisted(provider::class.simpleName ?: "Unknown")
        }

        for (provider in filteredProviders) {
            if (allResults.size >= limit) break
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                delay(200)
                val results = RetryHelper.withRetry(providerName) {
                    provider.getTopLosers(limit - allResults.size)
                }
                if (results.isNotEmpty()) {
                    allResults.addAll(results)
                    com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Losers", "Success", true, 0)
                }
            } catch (e: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Losers", e.message ?: "Error", false, 0)
            }
        }
        return allResults.toList()
    }

    suspend fun getMostActive(limit: Int = 10): List<String> {
        val allResults = mutableSetOf<String>()
        val filteredProviders = providers.screenerProviders.filter { provider ->
            !ProviderStatusManager.isBlacklisted(provider::class.simpleName ?: "Unknown")
        }

        for (provider in filteredProviders) {
            if (allResults.size >= limit) break
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                delay(200)
                val results = RetryHelper.withRetry(providerName) {
                    provider.getMostActive(limit - allResults.size)
                }
                if (results.isNotEmpty()) {
                    allResults.addAll(results)
                    com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Actives", "Success", true, 0)
                }
            } catch (e: Exception) {
                com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, "Actives", e.message ?: "Error", false, 0)
            }
        }
        return allResults.toList()
    }

    private suspend fun <P : Any, T> tryProviders(
        dataType: String,
        providers: List<P>,
        fetch: suspend (P) -> T,
        isValid: (T) -> Boolean
    ): T? {
        val filteredProviders = providers.filter { provider ->
            val providerName = provider::class.simpleName ?: "Unknown"
            !ProviderStatusManager.isBlacklisted(providerName)
        }

        if (filteredProviders.isEmpty() && providers.isNotEmpty()) {
            Logger.w("Repository", "All providers for $dataType are currently blacklisted due to errors")
        }

        val startTime = System.currentTimeMillis()
        for (provider in filteredProviders) {
            val providerName = provider::class.simpleName ?: "Unknown"
            try {
                // Add a small delay between provider calls/network requests to avoid rate limits
                delay(200)
                
                val result = RetryHelper.withRetry(providerName) {
                    fetch(provider)
                }
                if (isValid(result)) {
                    val duration = System.currentTimeMillis() - startTime
                    com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, dataType, "Success", true, duration)
                    Logger.d("Repository", "SUCCESS: $dataType from $providerName")
                    return result
                }
                Logger.d("Repository", "EMPTY: $dataType from $providerName")
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                com.polaralias.signalsynthesis.util.ActivityLogger.logApi(providerName, dataType, errorMessage, false, 0)
                
                // Handle 403 Forbidden - Blacklist for 10 minutes
                if (errorMessage.contains("403") || (e is retrofit2.HttpException && e.code() == 403)) {
                    Logger.e("Repository", "PROVIDER BLOCKED: $providerName returned 403 Forbidden. Blacklisting for 10 minutes.")
                    ProviderStatusManager.blacklistProvider(providerName, ENFORCED_COOLDOWN_MS)
                    Logger.event("provider_blacklisted", mapOf("provider" to providerName, "reason" to "403 Forbidden"))
                } else {
                    Logger.w("Repository", "FAILED: $dataType from $providerName: $errorMessage")
                }
            }
        }
        return null
    }



    companion object {
        private const val ENFORCED_COOLDOWN_MS = 10 * 60 * 1000L // 10 minutes
    }

    fun clearAllCaches() {
        quoteCache.clear()
        intradayCache.clear()
        dailyCache.clear()
        profileCache.clear()
        metricsCache.clear()
        sentimentCache.clear()
    }
}
