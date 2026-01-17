package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.*
import com.polaralias.signalsynthesis.domain.provider.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class RunAnalysisUseCaseTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneId.of("UTC"))

    @Test
    fun executeRunsFullPipeline() = runTest {
        // Setup data for AAPL (which is returned by DiscoverCandidatesUseCase)
        val symbol = "AAPL"
        val quote = Quote(symbol, 150.0, 10_000_000L, Instant.now(fixedClock))
        val intradayBars = createIntradayBars(symbol, 20)
        val dailyBars = createDailyBars(symbol, 200)
        val profile = CompanyProfile("Apple Inc.", "Technology", "Consumer Electronics", "Desc")
        val metrics = FinancialMetrics(2_000_000_000_000L, 30.0, 5.0)
        val sentiment = SentimentData(0.5, "Bullish")

        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = listOf(StaticQuoteProvider(mapOf(symbol to quote))),
                intradayProviders = listOf(StaticIntradayProvider(mapOf(symbol to intradayBars))),
                dailyProviders = listOf(StaticDailyProvider(mapOf(symbol to dailyBars))),
                profileProviders = listOf(StaticProfileProvider(mapOf(symbol to profile))),
                metricsProviders = listOf(StaticMetricsProvider(mapOf(symbol to metrics))),
                sentimentProviders = listOf(StaticSentimentProvider(mapOf(symbol to sentiment)))
            )
        )

        val useCase = RunAnalysisUseCase(repository, fixedClock)

        // Execute for SWING trading (triggers EOD enrichment)
        val result = useCase.execute(TradingIntent.SWING)

        assertEquals(TradingIntent.SWING, result.intent)
        // Candidates are many, but we only have data for AAPL, so others might fail or be filtered if Quote fails
        // Actually Repository.getQuotes returns what is available.
        // FilterTradeable uses quotes. If quotes are missing for others, they are filtered out?
        // Let's check FilterTradeable logic. It likely filters based on available quotes.

        // Assuming DiscoverCandidates returns AAPL + others.
        // Repository returns Quote only for AAPL.
        // FilterTradeable likely keeps AAPL (valid price/vol) and drops others (if quote missing or invalid).

        assertTrue(result.tradeableCount >= 1)
        assertTrue(result.setupCount >= 1)

        val setup = result.setups.find { it.symbol == symbol }
        assertTrue(setup != null)
        assertEquals("AAPL", setup?.symbol)
    }

    private fun createIntradayBars(symbol: String, count: Int): List<IntradayBar> {
        val bars = mutableListOf<IntradayBar>()
        val start = Instant.parse("2026-01-01T10:00:00Z")
        for (i in 0 until count) {
            bars.add(IntradayBar(symbol, 150.0, 155.0, 145.0, 150.0, 1000, start.plusSeconds(i * 60L)))
        }
        return bars
    }

    private fun createDailyBars(symbol: String, count: Int): List<DailyBar> {
        val bars = mutableListOf<DailyBar>()
        val start = Instant.parse("2025-01-01T10:00:00Z")
        for (i in 0 until count) {
            bars.add(DailyBar(symbol, 150.0, 155.0, 145.0, 150.0, 1000000, start.plusSeconds(i * 86400L)))
        }
        return bars
    }

    // Static Providers
    class StaticQuoteProvider(private val data: Map<String, Quote>) : QuoteProvider {
        override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> = data.filterKeys { symbols.contains(it) }
    }
    class StaticIntradayProvider(private val data: Map<String, List<IntradayBar>>) : IntradayProvider {
        override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> = data[symbol] ?: emptyList()
    }
    class StaticDailyProvider(private val data: Map<String, List<DailyBar>>) : DailyProvider {
        override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> = data[symbol] ?: emptyList()
    }
    class StaticProfileProvider(private val data: Map<String, CompanyProfile>) : ProfileProvider {
        override suspend fun getProfile(symbol: String): CompanyProfile? = data[symbol]
    }
    class StaticMetricsProvider(private val data: Map<String, FinancialMetrics>) : MetricsProvider {
        override suspend fun getMetrics(symbol: String): FinancialMetrics? = data[symbol]
    }
    class StaticSentimentProvider(private val data: Map<String, SentimentData>) : SentimentProvider {
        override suspend fun getSentiment(symbol: String): SentimentData? = data[symbol]
    }
}
