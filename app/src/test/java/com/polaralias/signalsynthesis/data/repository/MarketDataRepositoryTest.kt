package com.polaralias.signalsynthesis.data.repository

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MarketDataRepositoryTest {

    @Test
    fun getQuotesFallsBackToNextProvider() = runTest {
        val fallbackQuote = Quote(
            symbol = "AAA",
            price = 10.0,
            volume = 100L,
            timestamp = Instant.parse("2026-01-01T00:00:00Z")
        )
        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = listOf(ThrowingQuoteProvider(), StaticQuoteProvider(mapOf("AAA" to fallbackQuote))),
                intradayProviders = emptyList(),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )

        val quotes = repository.getQuotes(listOf("AAA"))

        assertEquals(1, quotes.size)
        assertEquals(fallbackQuote, quotes["AAA"])
    }

    @Test
    fun getQuotesReturnsCachedResult() = runTest {
        val symbol = "BBB"
        val quote = Quote(symbol, 20.0, 200L, Instant.parse("2026-01-01T00:00:00Z"))
        val provider = CountingQuoteProvider(mapOf(symbol to quote))

        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = listOf(provider),
                intradayProviders = emptyList(),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )

        // First call should hit provider
        val result1 = repository.getQuotes(listOf(symbol))
        assertEquals(quote, result1[symbol])
        assertEquals(1, provider.callCount)

        // Second call should use cache
        val result2 = repository.getQuotes(listOf(symbol))
        assertEquals(quote, result2[symbol])
        assertEquals(1, provider.callCount) // Count should remain 1
    }

    private class ThrowingQuoteProvider : QuoteProvider {
        override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
            throw IllegalStateException("Provider failure")
        }
    }

    private class StaticQuoteProvider(
        private val quotes: Map<String, Quote>
    ) : QuoteProvider {
        override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
            return quotes.filterKeys { symbols.contains(it) }
        }
    }

    private class CountingQuoteProvider(
        private val quotes: Map<String, Quote>
    ) : QuoteProvider {
        var callCount = 0
        override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
            callCount++
            return quotes.filterKeys { symbols.contains(it) }
        }
    }
}
