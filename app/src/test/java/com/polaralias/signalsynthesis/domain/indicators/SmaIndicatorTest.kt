package com.polaralias.signalsynthesis.domain.indicators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmaIndicatorTest {

    @Test
    fun calculateReturnsAverageOfLastPeriod() {
        val prices = (1..10).map { it.toDouble() }
        val sma = SmaIndicator.calculate(prices, period = 5)

        assertEquals(8.0, sma, 1e-6)
    }

    @Test
    fun calculateReturnsNullForInsufficientData() {
        val prices = listOf(1.0, 2.0, 3.0)
        val sma = SmaIndicator.calculate(prices, period = 5)

        assertNull(sma)
    }

    @Test
    fun calculateMultipleReturnsExpectedPeriods() {
        val prices = (1..10).map { it.toDouble() }
        val result = SmaIndicator.calculateMultiple(
            bars = prices.mapIndexed { index, close ->
                com.polaralias.signalsynthesis.domain.model.DailyBar(
                    date = java.time.LocalDate.of(2026, 1, 1).plusDays(index.toLong()),
                    open = close,
                    high = close,
                    low = close,
                    close = close,
                    volume = 100L
                )
            },
            periods = listOf(5, 10)
        )

        assertEquals(8.0, result[5], 1e-6)
        assertEquals(5.5, result[10], 1e-6)
    }
}
