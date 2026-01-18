package com.polaralias.signalsynthesis.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.polaralias.signalsynthesis.data.db.AppDatabase
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
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
            totalCandidates = 0,
            tradeableCount = 0,
            setupCount = 0,
            setups = emptyList(),
            generatedAt = Instant.now()
        )
        repository.saveHistory(result)
        val history = repository.getHistory().first()
        assertEquals(1, history.size)
        assertEquals(TradingIntent.DAY_TRADE, history[0].intent)
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
