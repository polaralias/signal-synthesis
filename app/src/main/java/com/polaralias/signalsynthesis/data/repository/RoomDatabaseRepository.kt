package com.polaralias.signalsynthesis.data.repository

import com.polaralias.signalsynthesis.data.db.dao.HistoryDao
import com.polaralias.signalsynthesis.data.db.dao.WatchlistDao
import com.polaralias.signalsynthesis.data.db.entity.HistoryEntity
import com.polaralias.signalsynthesis.data.db.entity.WatchlistEntity
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomDatabaseRepository(
    private val watchlistDao: WatchlistDao,
    private val historyDao: HistoryDao
) : DatabaseRepository {
    private val moshi = Moshi.Builder()
        .add(InstantAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, TradeSetup::class.java)
    private val adapter = moshi.adapter<List<TradeSetup>>(listType)

    override suspend fun addToWatchlist(symbol: String) {
        watchlistDao.insert(WatchlistEntity(symbol, System.currentTimeMillis()))
    }

    override suspend fun removeFromWatchlist(symbol: String) {
        watchlistDao.delete(symbol)
    }

    override fun getWatchlist(): Flow<List<String>> {
        return watchlistDao.getAll().map { list -> list.map { it.symbol } }
    }

    override suspend fun saveHistory(result: AnalysisResult) {
        val json = adapter.toJson(result.setups)
        historyDao.insert(
            HistoryEntity(
                generatedAt = result.generatedAt.toEpochMilli(),
                intent = result.intent.name,
                setupsJson = json
            )
        )
    }

    override fun getHistory(): Flow<List<AnalysisResult>> {
        return historyDao.getAll().map { list ->
            list.map { entity ->
                val setups = adapter.fromJson(entity.setupsJson) ?: emptyList()
                AnalysisResult(
                    intent = TradingIntent.valueOf(entity.intent),
                    totalCandidates = setups.size,
                    tradeableCount = setups.size,
                    setupCount = setups.size,
                    setups = setups,
                    generatedAt = Instant.ofEpochMilli(entity.generatedAt)
                )
            }
        }
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll()
    }
}

private class InstantAdapter {
    @ToJson
    fun toJson(instant: Instant): String = instant.toString()

    @FromJson
    fun fromJson(value: String): Instant = Instant.parse(value)
}
