package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FilterTradeableUseCaseTest {

    @Test
    fun executeFiltersOnPriceAndVolume() = runTest {
        val quotes = mapOf(
            "AAA" to Quote("AAA", price = 1.5, volume = 100L, timestamp = Instant.parse("2026-01-01T00:00:00Z")),
            "BBB" to Quote("BBB", price = 0.5, volume = 100L, timestamp = Instant.parse("2026-01-01T00:00:00Z")),
            "CCC" to Quote("CCC", price = 2.0, volume = 0L, timestamp = Instant.parse("2026-01-01T00:00:00Z"))
        )
        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = listOf(StaticQuoteProvider(quotes)),
                intradayProviders = emptyList(),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )
        val useCase = FilterTradeableUseCase(repository)

        val result = useCase.execute(listOf("AAA", "BBB", "CCC"))

        assertEquals(listOf("AAA"), result)
    }

    private class StaticQuoteProvider(
        private val quotes: Map<String, Quote>
    ) : QuoteProvider {
        override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
            return quotes.filterKeys { symbols.contains(it) }
        }
    }
}
