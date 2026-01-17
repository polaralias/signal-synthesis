package com.polaralias.signalsynthesis.domain.indicators

import com.polaralias.signalsynthesis.domain.model.IntradayBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class VwapIndicatorTest {

    @Test
    fun calculateUsesTypicalPrice() {
        val bars = listOf(
            IntradayBar(
                time = Instant.parse("2026-01-01T10:00:00Z"),
                open = 9.0,
                high = 10.0,
                low = 8.0,
                close = 9.0,
                volume = 100L
            ),
            IntradayBar(
                time = Instant.parse("2026-01-01T10:01:00Z"),
                open = 10.0,
                high = 12.0,
                low = 9.0,
                close = 11.0,
                volume = 200L
            )
        )

        val expected = (9.0 * 100.0 + ((12.0 + 9.0 + 11.0) / 3.0) * 200.0) / 300.0
        val result = VwapIndicator.calculate(bars)

        assertEquals(expected, result, 1e-6)
    }

    @Test
    fun calculateFromCloseUsesClosePrice() {
        val bars = listOf(
            IntradayBar(
                time = Instant.parse("2026-01-01T10:00:00Z"),
                open = 9.0,
                high = 10.0,
                low = 8.0,
                close = 9.0,
                volume = 100L
            ),
            IntradayBar(
                time = Instant.parse("2026-01-01T10:01:00Z"),
                open = 10.0,
                high = 12.0,
                low = 9.0,
                close = 11.0,
                volume = 200L
            )
        )

        val expected = (9.0 * 100.0 + 11.0 * 200.0) / 300.0
        val result = VwapIndicator.calculateFromClose(bars)

        assertEquals(expected, result, 1e-6)
    }

    @Test
    fun calculateReturnsNullForEmptyBars() {
        assertNull(VwapIndicator.calculate(emptyList()))
    }
}
