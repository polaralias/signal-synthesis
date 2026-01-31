package com.polaralias.signalsynthesis.data.db.dao

import androidx.room.*
import com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiSummaryDao {
    @Query("SELECT * FROM ai_summaries WHERE symbol = :symbol AND model = :model AND promptHash = :promptHash")
    suspend fun getByKey(symbol: String, model: String, promptHash: String): AiSummaryEntity?

    @Query("SELECT * FROM ai_summaries")
    fun getAll(): Flow<List<AiSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: AiSummaryEntity)

    @Delete
    suspend fun delete(summary: AiSummaryEntity)

    @Query("DELETE FROM ai_summaries")
    suspend fun clear()
}
