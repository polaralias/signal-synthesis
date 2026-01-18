package com.polaralias.signalsynthesis.data.repository

import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import kotlinx.coroutines.flow.Flow

interface DatabaseRepository {
    suspend fun addToWatchlist(symbol: String)
    suspend fun removeFromWatchlist(symbol: String)
    fun getWatchlist(): Flow<List<String>>
    suspend fun saveHistory(result: AnalysisResult)
    fun getHistory(): Flow<List<AnalysisResult>>
    suspend fun clearHistory()
}
