package com.polaralias.signalsynthesis.data.repository

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.provider.ProviderStatusManager
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.Headers
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response.Builder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
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
        ProviderStatusManager.clearForTesting()
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

    @Test
    fun getQuotesBlacklistsRateLimitedProviderAndSkipsItOnNextRequest() = runTest {
        ProviderStatusManager.clearForTesting()
        val firstSymbol = "AAA"
        val secondSymbol = "BBB"
        val firstQuote = Quote(firstSymbol, 10.0, 100L, Instant.parse("2026-01-01T00:00:00Z"))
        val secondQuote = Quote(secondSymbol, 11.0, 120L, Instant.parse("2026-01-01T00:00:00Z"))
        val rateLimitedProvider = RateLimitedQuoteProvider()
        val fallbackProvider = CountingQuoteProvider(
            mapOf(
                firstSymbol to firstQuote,
                secondSymbol to secondQuote
            )
        )

        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = listOf(rateLimitedProvider, fallbackProvider),
                intradayProviders = emptyList(),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )

        val firstResult = repository.getQuotes(listOf(firstSymbol))
        val secondResult = repository.getQuotes(listOf(secondSymbol))

        assertEquals(firstQuote, firstResult[firstSymbol])
        assertEquals(secondQuote, secondResult[secondSymbol])
        assertEquals(3, rateLimitedProvider.callCount)
        assertEquals(2, fallbackProvider.callCount)
        assertTrue(ProviderStatusManager.isBlacklisted(RateLimitedQuoteProvider::class.simpleName ?: "Unknown"))
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

    private class RateLimitedQuoteProvider : QuoteProvider {
        var callCount = 0

        override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
            callCount++
            val rawResponse = Builder()
                .request(Request.Builder().url("https://example.com/quotes").build())
                .protocol(Protocol.HTTP_1_1)
                .code(429)
                .message("Too Many Requests")
                .headers(Headers.headersOf("Retry-After", "0"))
                .build()
            val response = Response.error<Any>(
                "{}".toResponseBody("application/json".toMediaType()),
                rawResponse
            )
            throw HttpException(response)
        }
    }
}
