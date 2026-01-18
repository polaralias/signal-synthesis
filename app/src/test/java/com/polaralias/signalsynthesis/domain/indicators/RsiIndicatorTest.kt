package com.polaralias.signalsynthesis.domain.indicators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RsiIndicatorTest {

    @Test
    fun calculateReturns100ForAllGains() {
        val prices = (1..16).map { it.toDouble() }
        val rsi = RsiIndicator.calculate(prices, period = 14)

        assertEquals(100.0, rsi!!, 1e-6)
    }

    @Test
    fun calculateReturns50ForFlatSeries() {
        val prices = List(15) { 10.0 }
        val rsi = RsiIndicator.calculate(prices, period = 14)

        assertEquals(50.0, rsi!!, 1e-6)
    }

    @Test
    fun calculateReturnsNullForInsufficientData() {
        val prices = List(10) { 10.0 }
        val rsi = RsiIndicator.calculate(prices, period = 14)

        assertNull(rsi)
    }
}
