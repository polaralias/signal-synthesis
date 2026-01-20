package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.provider.ProviderBundle
import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverCandidatesUseCaseTest {

    private val emptyBundle = ProviderBundle.empty()
    private val repository = MarketDataRepository(emptyBundle)
    private val useCase = DiscoverCandidatesUseCase(repository)

    @Test
    fun returnsCandidatesForDayTrading() = runTest {
        val result = useCase.execute(TradingIntent.DAY_TRADE)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("AAPL"))
    }

    @Test
    fun returnsCandidatesForSwingTrading() = runTest {
        val result = useCase.execute(TradingIntent.SWING)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("AAPL"))
    }

    @Test
    fun returnsCandidatesForLongTerm() = runTest {
        val result = useCase.execute(TradingIntent.LONG_TERM)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("AAPL"))
    }
}
