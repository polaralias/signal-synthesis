package com.polaralias.signalsynthesis.domain.indicators

import com.polaralias.signalsynthesis.domain.model.IntradayBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class AtrIndicatorTest {

    @Test
    fun calculateFromIntradayReturnsConstantRange() {
        val bars = (0..14).map { index ->
            IntradayBar(
                time = Instant.parse("2026-01-01T10:00:00Z").plusSeconds(index.toLong() * 60),
                open = 9.0,
                high = 10.0,
                low = 8.0,
                close = 9.0,
                volume = 100L
            )
        }

        val atr = AtrIndicator.calculateFromIntraday(bars, period = 14)

        assertEquals(2.0, atr, 1e-6)
    }

    @Test
    fun calculateFromIntradayReturnsNullForInsufficientData() {
        val bars = (0..5).map { index ->
            IntradayBar(
                time = Instant.parse("2026-01-01T10:00:00Z").plusSeconds(index.toLong() * 60),
                open = 9.0,
                high = 10.0,
                low = 8.0,
                close = 9.0,
                volume = 100L
            )
        }

        val atr = AtrIndicator.calculateFromIntraday(bars, period = 14)

        assertNull(atr)
    }
}
