package com.polaralias.signalsynthesis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.polaralias.signalsynthesis.data.db.dao.HistoryDao
import com.polaralias.signalsynthesis.data.db.dao.WatchlistDao
import com.polaralias.signalsynthesis.data.db.entity.HistoryEntity
import com.polaralias.signalsynthesis.data.db.entity.WatchlistEntity

@Database(entities = [WatchlistEntity::class, HistoryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun historyDao(): HistoryDao
}
