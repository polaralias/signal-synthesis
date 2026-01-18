package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import com.polaralias.signalsynthesis.domain.provider.IntradayProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant

class EnrichIntradayUseCaseTest {

    @Test
    fun executeCalculatesIndicators() = runTest {
        val symbol = "TEST"
        val bars = createBars(symbol, 20) // Enough for RSI-14

        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = emptyList(),
                intradayProviders = listOf(StaticIntradayProvider(mapOf(symbol to bars))),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )

        val useCase = EnrichIntradayUseCase(repository)
        val result = useCase.execute(listOf(symbol))

        val stats = result[symbol]
        assertNotNull(stats)
        assertNotNull(stats?.vwap)
        assertNotNull(stats?.rsi14)
        assertNotNull(stats?.atr14)
    }

    @Test
    fun executeHandlesMissingData() = runTest {
        val symbol = "MISSING"

        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = emptyList(),
                intradayProviders = listOf(StaticIntradayProvider(emptyMap())),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )

        val useCase = EnrichIntradayUseCase(repository)
        val result = useCase.execute(listOf(symbol))

        assertEquals(null, result[symbol])
    }

    private fun createBars(symbol: String, count: Int): List<IntradayBar> {
        val bars = mutableListOf<IntradayBar>()
        val start = Instant.parse("2026-01-01T10:00:00Z")
        for (i in 0 until count) {
            bars.add(
                IntradayBar(
                    time = start.plusSeconds(i * 60L),
                    open = 100.0 + i,
                    high = 105.0 + i,
                    low = 95.0 + i,
                    close = 102.0 + i,
                    volume = 1000
                )
            )
        }
        return bars
    }

    private class StaticIntradayProvider(
        private val data: Map<String, List<IntradayBar>>
    ) : IntradayProvider {
        override suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar> {
            return data[symbol] ?: emptyList()
        }
    }
}
