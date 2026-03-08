package com.polaralias.signalsynthesis.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.polaralias.signalsynthesis.data.db.AppDatabase
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.DecisionUpdate
import com.polaralias.signalsynthesis.domain.model.KeepItem
import com.polaralias.signalsynthesis.domain.model.RssDigest
import com.polaralias.signalsynthesis.domain.model.RssHeadline
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RoomDatabaseRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomDatabaseRepository(db.watchlistDao(), db.historyDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun watchlist_addAndRemove() = runBlocking {
        repository.addToWatchlist("AAPL")
        var watchlist = repository.getWatchlist().first()
        assertTrue(watchlist.contains("AAPL"))

        repository.removeFromWatchlist("AAPL")
        watchlist = repository.getWatchlist().first()
        assertTrue(watchlist.isEmpty())
    }

    @Test
    fun history_saveAndGet() = runBlocking {
        val result = AnalysisResult(
            intent = TradingIntent.DAY_TRADE,
            totalCandidates = 12,
            tradeableCount = 7,
            setupCount = 1,
            setups = listOf(
                TradeSetup(
                    symbol = "AAPL",
                    setupType = "High Probability",
                    triggerPrice = 100.0,
                    stopLoss = 95.0,
                    targetPrice = 110.0,
                    confidence = 0.8,
                    reasons = listOf("Momentum"),
                    validUntil = Instant.parse("2026-01-01T12:00:00Z"),
                    intent = TradingIntent.DAY_TRADE
                )
            ),
            generatedAt = Instant.parse("2026-01-01T10:00:00Z"),
            globalNotes = listOf("Keep an eye on liquidity"),
            rssDigest = RssDigest(
                mapOf(
                    "AAPL" to listOf(RssHeadline(title = "Apple headline", link = "https://example.com", publishedAt = "2026-01-01T09:00:00Z"))
                )
            ),
            decisionUpdate = DecisionUpdate(
                keep = listOf(KeepItem(symbol = "AAPL", confidence = 0.9, setupBias = "Bullish"))
            )
        )
        repository.saveHistory(result)
        val history = repository.getHistory().first()
        assertEquals(1, history.size)
        assertEquals(TradingIntent.DAY_TRADE, history[0].intent)
        assertEquals(12, history[0].totalCandidates)
        assertEquals(7, history[0].tradeableCount)
        assertEquals(listOf("Keep an eye on liquidity"), history[0].globalNotes)
        assertEquals("AAPL", history[0].decisionUpdate?.keep?.firstOrNull()?.symbol)
        assertEquals("Apple headline", history[0].rssDigest?.itemsBySymbol?.get("AAPL")?.firstOrNull()?.title)
    }

    @Test
    fun history_clear() = runBlocking {
        val result = AnalysisResult(
            intent = TradingIntent.LONG_TERM,
            totalCandidates = 0,
            tradeableCount = 0,
            setupCount = 0,
            setups = emptyList(),
            generatedAt = Instant.now()
        )
        repository.saveHistory(result)
        repository.clearHistory()
        val history = repository.getHistory().first()
        assertTrue(history.isEmpty())
    }
}
