package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.domain.model.EodStats
import com.polaralias.signalsynthesis.domain.model.IntradayStats
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RankSetupsUseCaseTest {

    @Test
    fun executeRanksByConfidenceAndSetsFields() {
        val fixedInstant = Instant.parse("2026-01-01T12:00:00Z")
        val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
        val useCase = RankSetupsUseCase(clock)

        val symbols = listOf("AAA", "BBB")
        val quotes = mapOf(
            "AAA" to Quote("AAA", price = 100.0, volume = 1_000_000, timestamp = fixedInstant),
            "BBB" to Quote("BBB", price = 50.0, volume = 1_000_000, timestamp = fixedInstant)
        )
        val intradayStats = mapOf(
            "AAA" to IntradayStats(vwap = 90.0, rsi14 = 25.0, atr14 = 1.2),
            "BBB" to IntradayStats(vwap = 55.0, rsi14 = 75.0, atr14 = 1.2)
        )
        val eodStats = mapOf(
            "AAA" to EodStats(sma50 = 95.0, sma200 = 95.0),
            "BBB" to EodStats(sma50 = 60.0, sma200 = 60.0)
        )
        val sentiment = mapOf("AAA" to 0.3, "BBB" to -0.1)

        val results = useCase.execute(
            symbols = symbols,
            quotes = quotes,
            intradayStats = intradayStats,
            eodStats = eodStats,
            sentimentScores = sentiment,
            intent = TradingIntent.DAY_TRADE
        )

        assertEquals("AAA", results.first().symbol)
        assertEquals("High Probability", results.first().setupType)
        assertEquals(1.0, results.first().confidence, 1e-6)
        assertEquals(fixedInstant.plusSeconds(30 * 60), results.first().validUntil)
        assertTrue(results.first().reasons.any { it.startsWith("Price above VWAP") })
        assertTrue(results.first().reasons.any { it.startsWith("RSI oversold") })
        assertTrue(results.first().reasons.any { it.startsWith("Price above SMA-200") })
        assertTrue(results.first().reasons.any { it.startsWith("Positive sentiment") })
    }
}
