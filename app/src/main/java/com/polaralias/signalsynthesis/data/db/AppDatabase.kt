package com.polaralias.signalsynthesis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.polaralias.signalsynthesis.data.db.dao.AiSummaryDao
import com.polaralias.signalsynthesis.data.db.dao.HistoryDao
import com.polaralias.signalsynthesis.data.db.dao.WatchlistDao
import com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity
import com.polaralias.signalsynthesis.data.db.entity.HistoryEntity
import com.polaralias.signalsynthesis.data.db.entity.WatchlistEntity
import com.polaralias.signalsynthesis.data.rss.RssDao
import com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity
import com.polaralias.signalsynthesis.data.rss.RssItemEntity

@Database(
    entities = [
        WatchlistEntity::class,
        HistoryEntity::class,
        AiSummaryEntity::class,
        RssFeedStateEntity::class,
        RssItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun historyDao(): HistoryDao
    abstract fun aiSummaryDao(): AiSummaryDao
    abstract fun rssDao(): RssDao
}
