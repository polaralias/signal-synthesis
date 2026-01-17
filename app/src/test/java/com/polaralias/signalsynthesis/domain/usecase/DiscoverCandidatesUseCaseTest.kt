package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.domain.model.TradingIntent
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverCandidatesUseCaseTest {

    private val useCase = DiscoverCandidatesUseCase()

    @Test
    fun returnsCandidatesForDayTrading() {
        val result = useCase.execute(TradingIntent.DAY_TRADE)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("AAPL"))
    }

    @Test
    fun returnsCandidatesForSwingTrading() {
        val result = useCase.execute(TradingIntent.SWING)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("AAPL"))
    }

    @Test
    fun returnsCandidatesForLongTerm() {
        val result = useCase.execute(TradingIntent.LONG_TERM)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("AAPL"))
    }
}
