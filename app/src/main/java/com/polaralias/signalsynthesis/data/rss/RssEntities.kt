package com.polaralias.signalsynthesis.data.rss

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_feed_states")
data class RssFeedStateEntity(
    @PrimaryKey val feedUrl: String,
    val etag: String? = null,
    val lastModified: String? = null,
    val lastFetchedAt: Long = 0
)

@Entity(tableName = "rss_items")
data class RssItemEntity(
    @PrimaryKey val guidHash: String,
    val feedUrl: String,
    val title: String,
    val link: String,
    val publishedAt: Long,
    val snippet: String,
    val fetchedAt: Long
)
