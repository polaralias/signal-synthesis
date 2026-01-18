package com.polaralias.signalsynthesis.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.polaralias.signalsynthesis.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM analysis_history ORDER BY generatedAt DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM analysis_history")
    suspend fun deleteAll()
}
