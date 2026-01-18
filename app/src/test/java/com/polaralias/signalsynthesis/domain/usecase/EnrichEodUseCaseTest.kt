package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.provider.DailyProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class EnrichEodUseCaseTest {

    @Test
    fun executeCalculatesIndicators() = runTest {
        val symbol = "TEST"
        val bars = createBars(symbol, 200) // Enough for SMA-200

        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = emptyList(),
                intradayProviders = emptyList(),
                dailyProviders = listOf(StaticDailyProvider(mapOf(symbol to bars))),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )

        val useCase = EnrichEodUseCase(repository)
        val result = useCase.execute(listOf(symbol))

        val stats = result[symbol]
        assertNotNull(stats)
        assertNotNull(stats?.sma50)
        assertNotNull(stats?.sma200)
    }

    @Test
    fun executeHandlesMissingData() = runTest {
        val symbol = "MISSING"

        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = emptyList(),
                intradayProviders = emptyList(),
                dailyProviders = listOf(StaticDailyProvider(emptyMap())),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        )

        val useCase = EnrichEodUseCase(repository)
        val result = useCase.execute(listOf(symbol))

        assertEquals(null, result[symbol])
    }

    private fun createBars(symbol: String, count: Int): List<DailyBar> {
        val bars = mutableListOf<DailyBar>()
        val start = Instant.parse("2025-01-01T10:00:00Z")
        for (i in 0 until count) {
            bars.add(
                DailyBar(
                    date = LocalDate.ofInstant(start.plusSeconds(i * 86400L), ZoneOffset.UTC),
                    open = 100.0 + i,
                    high = 105.0 + i,
                    low = 95.0 + i,
                    close = 102.0 + i,
                    volume = 1000000
                )
            )
        }
        return bars
    }

    private class StaticDailyProvider(
        private val data: Map<String, List<DailyBar>>
    ) : DailyProvider {
        override suspend fun getDaily(symbol: String, days: Int): List<DailyBar> {
            return data[symbol] ?: emptyList()
        }
    }
}
