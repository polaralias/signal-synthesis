package com.polaralias.signalsynthesis.data.provider.fmp

import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.domain.provider.DailyProvider
import com.polaralias.signalsynthesis.domain.provider.IntradayProvider
import com.polaralias.signalsynthesis.domain.provider.MetricsProvider
import com.polaralias.signalsynthesis.domain.provider.ProfileProvider
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import com.polaralias.signalsynthesis.domain.provider.SentimentProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class FmpMarketDataProvider(
    private val apiKey: String,
    private val service: FmpService = FmpService.create(),
    private val clock: Clock = Clock.systemUTC()
) : QuoteProvider,
    IntradayProvider,
    DailyProvider,
    ProfileProvider,
    MetricsProvider,
    SentimentProvider,
    com.polaralias.signalsynthesis.domain.provider.ScreenerProvider,
    com.polaralias.signalsynthesis.domain.provider.SearchProvider {

    override suspend fun searchSymbols(query: String, limit: Int): List<com.polaralias.signalsynthesis.domain.provider.SearchResult> {
        if (query.isBlank()) return emptyList()
        return try {
            val results = service.searchTickers(query, limit, apiKey)
            results.map {
                com.polaralias.signalsynthesis.domain.provider.SearchResult(
                    symbol = it.symbol ?: "",
                    name = it.name ?: "",
                    exchange = it.exchangeShortName ?: it.stockExchange
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun screenStocks(
        minPrice: Double?,
        maxPrice: Double?,
        minVolume: Long?,
        sector: String?,
        limit: Int
    ): List<String> {
        return try {
            val results = service.stockScreener(
                priceMin = minPrice,
                priceMax = maxPrice,
                volumeMin = minVolume,
                sector = sector,
                limit = limit,
                apiKey = apiKey
            )
            results.mapNotNull { it.symbol }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        if (symbols.isEmpty()) return emptyMap()
        return symbols.mapNotNull { symbol ->
            try {
                val quotes = service.getQuote(symbol, apiKey)
                quotes.firstOrNull()?.toQuote()
            } catch (e: Exception) {
                null
            }
        }.associateBy { it.symbol }
    }

    override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        return try {
            val (from, to) = getDateRange(days)
            val bars = service.getHistoricalChart("5min", symbol, from, to, apiKey)
            bars.toIntradayBars()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
        if (symbol.isBlank() || days <= 0) return emptyList()
        return try {
            val (from, to) = getDateRange(days)
            val bars = service.getHistoricalChart("1day", symbol, from, to, apiKey)
            bars.toDailyBars()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getProfile(symbol: String): CompanyProfile? {
        if (symbol.isBlank()) return null
        return try {
            val profiles = service.getProfile(symbol, apiKey)
            profiles.firstOrNull()?.let {
                CompanyProfile(
                    name = it.companyName ?: symbol,
                    sector = it.sector,
                    industry = it.industry,
                    description = it.description
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMetrics(symbol: String): FinancialMetrics? {
        if (symbol.isBlank()) return null
        return try {
            val metrics = service.getKeyMetrics(symbol, limit = 1, apiKey = apiKey)
            val quote = service.getQuote(symbol, apiKey).firstOrNull()
            
            metrics.firstOrNull()?.let {
                FinancialMetrics(
                    marketCap = it.marketCap,
                    peRatio = it.peRatio,
                    eps = it.netIncomePerShare,
                    earningsDate = quote?.earningsAnnouncement,
                    dividendYield = it.dividendYield,
                    pbRatio = it.pbRatio,
                    debtToEquity = it.debtToEquity
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getSentiment(symbol: String): SentimentData? {
        if (symbol.isBlank()) return null
        return try {
            val sentiments = service.getNewsSentiment(symbol, page = 0, apiKey = apiKey)
            if (sentiments.isEmpty()) return null
            
            // Aggregate sentiment from recent news
            val scores = sentiments.mapNotNull { it.sentimentScore }
            if (scores.isEmpty()) return null
            
            val avgScore = scores.average()
            // Normalize from FMP's scale (typically 0-1) to our scale (-1 to 1)
            val normalizedScore = (avgScore - 0.5) * 2.0
            
            SentimentData(
                score = normalizedScore.coerceIn(-1.0, 1.0),
                label = normalizedScore.toLabel()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getDateRange(days: Int): Pair<String, String> {
        val now = LocalDate.now(clock)
        val from = now.minusDays(days.toLong())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return Pair(from.format(formatter), now.format(formatter))
    }

    private fun FmpQuote.toQuote(): Quote? {
        val sym = symbol ?: return null
        val p = price ?: return null
        val vol = volume ?: 0L
        val ts = timestamp?.let { Instant.ofEpochSecond(it) } ?: Instant.now(clock)
        
        return Quote(
            symbol = sym,
            price = p,
            volume = vol,
            timestamp = ts
        )
    }

    private fun List<FmpChartBar>.toIntradayBars(): List<IntradayBar> {
        return mapNotNull { bar ->
            val dateStr = bar.date ?: return@mapNotNull null
            val open = bar.open ?: return@mapNotNull null
            val high = bar.high ?: return@mapNotNull null
            val low = bar.low ?: return@mapNotNull null
            val close = bar.close ?: return@mapNotNull null
            val volume = bar.volume ?: 0L
            
            val time = try {
                // FMP uses format like "2024-01-17 09:30:00"
                LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneOffset.UTC)
                    .toInstant()
            } catch (e: DateTimeParseException) {
                return@mapNotNull null
            }
            
            IntradayBar(
                time = time,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume
            )
        }
    }

    private fun List<FmpChartBar>.toDailyBars(): List<DailyBar> {
        return mapNotNull { bar ->
            val dateStr = bar.date ?: return@mapNotNull null
            val open = bar.open ?: return@mapNotNull null
            val high = bar.high ?: return@mapNotNull null
            val low = bar.low ?: return@mapNotNull null
            val close = bar.close ?: return@mapNotNull null
            val volume = bar.volume ?: 0L
            
            val date = try {
                // For daily bars, FMP might use "yyyy-MM-dd" or "yyyy-MM-dd HH:mm:ss"
                if (dateStr.contains(" ")) {
                    LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .toLocalDate()
                } else {
                    LocalDate.parse(dateStr)
                }
            } catch (e: DateTimeParseException) {
                return@mapNotNull null
            }
            
            DailyBar(
                date = date,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume
            )
        }
    }

    private fun Double.toLabel(): String {
        return when {
            this > 0.2 -> "Bullish"
            this < -0.2 -> "Bearish"
            else -> "Neutral"
        }
    }
}
