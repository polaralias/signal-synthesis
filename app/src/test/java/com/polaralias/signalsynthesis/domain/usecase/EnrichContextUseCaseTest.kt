package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.domain.provider.MetricsProvider
import com.polaralias.signalsynthesis.domain.provider.ProfileProvider
import com.polaralias.signalsynthesis.domain.provider.SentimentProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EnrichContextUseCaseTest {

    @Test
    fun executeContinuesThroughPartialFailures() = runTest {
        val repository = MarketDataRepository(
            ProviderBundle(
                quoteProviders = emptyList(),
                intradayProviders = emptyList(),
                dailyProviders = emptyList(),
                profileProviders = listOf(
                    object : ProfileProvider {
                        override suspend fun getProfile(symbol: String): CompanyProfile? {
                            if (symbol == "TSLA") error("profile failed")
                            return CompanyProfile("$symbol Inc.", "Tech", "Software", "desc")
                        }
                    }
                ),
                metricsProviders = listOf(
                    object : MetricsProvider {
                        override suspend fun getMetrics(symbol: String): FinancialMetrics? {
                            if (symbol == "AAPL") error("metrics failed")
                            return FinancialMetrics(1_000_000L, 20.0, 3.0, "2026-02-01")
                        }
                    }
                ),
                sentimentProviders = listOf(
                    object : SentimentProvider {
                        override suspend fun getSentiment(symbol: String): SentimentData? {
                            if (symbol == "AAPL") return null
                            return SentimentData(0.4, "Bullish")
                        }
                    }
                )
            )
        )

        val useCase = EnrichContextUseCase(repository)
        val result = useCase.execute(listOf("AAPL", "TSLA"))

        assertEquals(2, result.size)
        assertNotNull(result["AAPL"]?.profile)
        assertNull(result["AAPL"]?.metrics)
        assertNull(result["AAPL"]?.sentiment)
        assertNull(result["TSLA"]?.profile)
        assertNotNull(result["TSLA"]?.metrics)
        assertEquals("Bullish", result["TSLA"]?.sentiment?.label)
    }
}
