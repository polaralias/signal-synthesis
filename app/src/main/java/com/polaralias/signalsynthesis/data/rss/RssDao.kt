package com.polaralias.signalsynthesis.data.rss

import androidx.room.*

@Dao
interface RssDao {
    @Query("SELECT * FROM rss_feed_states WHERE feedUrl = :url")
    suspend fun getFeedState(url: String): RssFeedStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedState(state: RssFeedStateEntity)

    @Query("SELECT * FROM rss_items WHERE publishedAt >= :since ORDER BY publishedAt DESC")
    suspend fun getAllRecentItems(since: Long): List<RssItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<RssItemEntity>)

    @Query("DELETE FROM rss_items WHERE publishedAt < :threshold")
    suspend fun deleteOldItems(threshold: Long)
}
