package com.polaralias.signalsynthesis.data.repository

import com.polaralias.signalsynthesis.data.db.dao.AiSummaryDao
import com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity
import com.polaralias.signalsynthesis.domain.model.AiSynthesis
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiSummaryRepository(
    private val dao: AiSummaryDao,
    private val moshi: Moshi = Moshi.Builder().build()
) {
    private val listType = Types.newParameterizedType(List::class.java, String::class.java)
    private val adapter = moshi.adapter<List<String>>(listType)

    suspend fun getSummary(symbol: String): AiSynthesis? = withContext(Dispatchers.IO) {
        val entity = dao.getBySymbol(symbol) ?: return@withContext null
        AiSynthesis(
            summary = entity.summary,
            risks = adapter.fromJson(entity.risksJson) ?: emptyList(),
            verdict = entity.verdict
        )
    }

    suspend fun saveSummary(symbol: String, synthesis: AiSynthesis) = withContext(Dispatchers.IO) {
        val entity = AiSummaryEntity(
            symbol = symbol,
            summary = synthesis.summary,
            risksJson = adapter.toJson(synthesis.risks),
            verdict = synthesis.verdict,
            generatedAt = System.currentTimeMillis()
        )
        dao.insert(entity)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dao.clear()
    }
}
