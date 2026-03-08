package com.polaralias.signalsynthesis.data.provider.polygon

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MassiveMarketDataProviderTest {

    private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `screenStocks applies quote filters before returning symbols`() = runTest {
        val service = FakeMassiveService(
            tickers = listOf("CHEAP", "THIN", "VALID", "EXPENSIVE", "VALID2"),
            snapshots = mapOf(
                "CHEAP" to quoteSnapshot("CHEAP", price = 5.0, volume = 2_000_000),
                "THIN" to quoteSnapshot("THIN", price = 50.0, volume = 200_000),
                "VALID" to quoteSnapshot("VALID", price = 75.0, volume = 2_500_000),
                "EXPENSIVE" to quoteSnapshot("EXPENSIVE", price = 300.0, volume = 3_000_000),
                "VALID2" to quoteSnapshot("VALID2", price = 120.0, volume = 4_000_000)
            )
        )
        val provider = MassiveMarketDataProvider(
            apiKey = "test-key",
            primaryService = service,
            fallbackService = service,
            clock = clock
        )

        val results = provider.screenStocks(
            minPrice = 10.0,
            maxPrice = 200.0,
            minVolume = 1_000_000,
            sector = null,
            limit = 10
        )

        assertEquals(listOf("VALID", "VALID2"), results)
    }

    private fun quoteSnapshot(symbol: String, price: Double, volume: Long): MassiveSnapshotResponse {
        return MassiveSnapshotResponse(
            status = "OK",
            ticker = MassiveTickerSnapshot(
                ticker = symbol,
                day = MassiveDayData(close = price, volume = volume),
                lastTrade = MassiveTradeData(price = price, timestamp = clock.instant().toEpochMilli())
            )
        )
    }

    private class FakeMassiveService(
        private val tickers: List<String>,
        private val snapshots: Map<String, MassiveSnapshotResponse>
    ) : MassiveService {
        override suspend fun getSnapshot(ticker: String, apiKey: String): MassiveSnapshotResponse {
            return snapshots.getValue(ticker)
        }

        override suspend fun getAggregates(
            ticker: String,
            multiplier: Int,
            timespan: String,
            from: String,
            to: String,
            adjusted: Boolean,
            sort: String,
            limit: Int,
            apiKey: String
        ): MassiveAggregatesResponse {
            error("Unused in this test")
        }

        override suspend fun getTickerDetails(ticker: String, apiKey: String): MassiveTickerDetailsResponse {
            error("Unused in this test")
        }

        override suspend fun listTickers(
            search: String?,
            market: String,
            active: Boolean,
            type: String?,
            limit: Int,
            apiKey: String
        ): MassiveTickersResponse {
            return MassiveTickersResponse(
                status = "OK",
                results = tickers.take(limit).map { symbol ->
                    MassiveTickerResult(
                        ticker = symbol,
                        type = type
                    )
                }
            )
        }
    }
}
