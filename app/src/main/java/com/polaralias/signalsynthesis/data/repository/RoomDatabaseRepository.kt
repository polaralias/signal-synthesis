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
    private val tradeSetupListAdapter = moshi.adapter<List<TradeSetup>>(listType)
    private val resultAdapter = moshi.adapter(AnalysisResult::class.java)

    override suspend fun addToWatchlist(symbol: String, intent: TradingIntent?) {
        watchlistDao.insert(
            WatchlistEntity(
                symbol = symbol,
                intent = (intent ?: TradingIntent.DAY_TRADE).name,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeFromWatchlist(symbol: String) {
        watchlistDao.delete(symbol)
    }

    override fun getWatchlist(): Flow<List<String>> {
        return watchlistDao.getAll().map { list -> list.map { it.symbol } }
    }

    override suspend fun saveHistory(result: AnalysisResult) {
        val json = resultAdapter.toJson(result)
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
            list.map(::decodeHistoryEntity)
        }
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll()
    }

    private fun decodeHistoryEntity(entity: HistoryEntity): AnalysisResult {
        val storedResult = runCatching { resultAdapter.fromJson(entity.setupsJson) }.getOrNull()
        if (storedResult != null) {
            return storedResult
        }

        val legacySetups = tradeSetupListAdapter.fromJson(entity.setupsJson) ?: emptyList()
        return AnalysisResult(
            intent = TradingIntent.valueOf(entity.intent),
            totalCandidates = legacySetups.size,
            tradeableCount = legacySetups.size,
            setupCount = legacySetups.size,
            setups = legacySetups,
            generatedAt = Instant.ofEpochMilli(entity.generatedAt)
        )
    }
}

private class InstantAdapter {
    @ToJson
    fun toJson(instant: Instant): String = instant.toString()

    @FromJson
    fun fromJson(value: String): Instant = Instant.parse(value)
}
