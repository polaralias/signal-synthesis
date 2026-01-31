package com.polaralias.signalsynthesis.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.polaralias.signalsynthesis.data.db.dao.HistoryDao
import com.polaralias.signalsynthesis.data.db.dao.WatchlistDao
import com.polaralias.signalsynthesis.data.db.entity.HistoryEntity
import com.polaralias.signalsynthesis.data.db.entity.WatchlistEntity
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
class RoomDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var watchlistDao: WatchlistDao
    private lateinit var historyDao: HistoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        watchlistDao = db.watchlistDao()
        historyDao = db.historyDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun watchlistDao_insertAndGet() = runBlocking {
        val item = WatchlistEntity("AAPL", TradingIntent.DAY_TRADE.name, System.currentTimeMillis())
        watchlistDao.insert(item)
        val list = watchlistDao.getAll().first()
        assertEquals(1, list.size)
        assertEquals("AAPL", list[0].symbol)
    }

    @Test
    fun watchlistDao_delete() = runBlocking {
        val item = WatchlistEntity("TSLA", TradingIntent.SWING.name, System.currentTimeMillis())
        watchlistDao.insert(item)
        watchlistDao.delete("TSLA")
        val list = watchlistDao.getAll().first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun historyDao_insertAndGet() = runBlocking {
        val entity = HistoryEntity(
            generatedAt = Instant.now().toEpochMilli(),
            intent = TradingIntent.DAY_TRADE.name,
            setupsJson = "[]"
        )
        historyDao.insert(entity)
        val history = historyDao.getAll().first()
        assertEquals(1, history.size)
        assertEquals(TradingIntent.DAY_TRADE.name, history[0].intent)
    }

    @Test
    fun historyDao_clear() = runBlocking {
        val entity = HistoryEntity(
            generatedAt = Instant.now().toEpochMilli(),
            intent = TradingIntent.SWING.name,
            setupsJson = "[]"
        )
        historyDao.insert(entity)
        historyDao.deleteAll()
        val history = historyDao.getAll().first()
        assertTrue(history.isEmpty())
    }
}
