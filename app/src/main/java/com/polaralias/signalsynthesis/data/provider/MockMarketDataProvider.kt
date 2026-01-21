package com.polaralias.signalsynthesis.data.provider

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
import kotlin.math.max

class MockMarketDataProvider(
    private val clock: Clock = Clock.systemUTC()
) : QuoteProvider,
    IntradayProvider,
    DailyProvider,
    ProfileProvider,
    MetricsProvider,
    SentimentProvider,
    com.polaralias.signalsynthesis.domain.provider.ScreenerProvider {

    override suspend fun screenStocks(
        minPrice: Double?,
        maxPrice: Double?,
        minVolume: Long?,
        sector: String?,
        limit: Int
    ): List<String> {
        // Return a static list of popular tickers for mock data
        return listOf("AAPL", "NVDA", "AMD", "TSLA", "MSFT", "PLTR", "SOFI", "COIN")
    }

    override suspend fun getTopGainers(limit: Int): List<String> {
        return listOf("GME", "AMC", "RIOT", "MARA", "COIN").take(limit)
    }

    override suspend fun getTopLosers(limit: Int): List<String> {
        return listOf("INTC", "WBA", "DIS", "BA", "BA").take(limit)
    }

    override suspend fun getMostActive(limit: Int): List<String> {
        return listOf("TSLA", "NVDA", "AAPL", "AMD", "PLTR").take(limit)
    }

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        val now = Instant.now(clock)
        return symbols.mapIndexed { index, symbol ->
            val price = basePrice(index)
            symbol to Quote(
                symbol = symbol,
                price = price,
                volume = 1_500_000L + index * 100_000L,
                timestamp = now,
                changePercent = if (index % 2 == 0) 1.25 else -0.85
            )
        }.toMap()
    }

    override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
        if (days <= 0) return emptyList()
        val bars = max(1, days) * 30
        val now = Instant.now(clock)
        val base = basePrice(0)
        return (0 until bars).map { index ->
            val minutesAgo = ((bars - 1 - index) * 5L)
            val close = base + (index % 5 - 2) * 0.2
            val open = close - 0.05
            val high = close + 0.1
            val low = close - 0.1
            IntradayBar(
                time = now.minusSeconds(minutesAgo * 60),
                open = open,
                high = high,
                low = low,
                close = close,
                volume = 5_000L + index * 50L
            )
        }
    }

    override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
        if (days <= 0) return emptyList()
        val today = LocalDate.now(clock)
        return (0 until days).map { index ->
            val date = today.minusDays((days - 1 - index).toLong())
            val base = 80.0 + index * 0.4
            DailyBar(
                date = date,
                open = base - 0.3,
                high = base + 0.6,
                low = base - 0.8,
                close = base + 0.1,
                volume = 2_000_000L + index * 10_000L
            )
        }
    }

    override suspend fun getProfile(symbol: String): CompanyProfile? {
        return CompanyProfile(
            name = "$symbol Corp",
            sector = "Technology",
            industry = "Software",
            description = "Mock profile for $symbol."
        )
    }

    override suspend fun getMetrics(symbol: String): FinancialMetrics? {
        val nextEarnings = LocalDate.now(clock).plusDays(5).toString()
        return FinancialMetrics(
            marketCap = 125_000_000_000L,
            peRatio = 22.5,
            eps = 3.2,
            earningsDate = nextEarnings,
            dividendYield = 0.015,
            pbRatio = 3.8,
            debtToEquity = 0.45
        )
    }

    override suspend fun getSentiment(symbol: String): SentimentData? {
        return SentimentData(
            score = 0.05,
            label = "Neutral"
        )
    }

    private fun basePrice(index: Int): Double {
        return 100.0 + index * 4.25
    }
}
