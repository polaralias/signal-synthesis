package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.rss.RssDao
import com.polaralias.signalsynthesis.data.rss.RssFeedClient
import com.polaralias.signalsynthesis.data.rss.RssItemEntity
import com.polaralias.signalsynthesis.domain.model.RssDigest
import com.polaralias.signalsynthesis.domain.model.RssHeadline
import java.time.Instant
import java.time.temporal.ChronoUnit

class BuildRssDigestUseCase(
    private val rssFeedClient: RssFeedClient,
    private val rssDao: RssDao
) {
    /**
     * Pulls latest RSS feeds, caches them, and builds a digest for the given tickers.
     * 
     * @param tickers List of stock symbols to match.
     * @param feedUrls List of RSS/Atom feed URLs to fetch.
     * @param timeWindowHours How far back to look for items.
     * @param maxItemsPerTicker Limit of headlines per ticker.
     */
    suspend fun execute(
        tickers: List<String>,
        feedUrls: List<String>,
        timeWindowHours: Int = 48,
        maxItemsPerTicker: Int = 3,
        onProgress: ((String) -> Unit)? = null
    ): RssDigest {
        // 1. Fetch and update all feeds
        val totalFeeds = feedUrls.size.coerceAtLeast(1)
        feedUrls.forEachIndexed { index, url ->
            onProgress?.invoke("RSS search ${index + 1}/$totalFeeds")
            rssFeedClient.fetchFeed(url)
        }
        
        // 2. Clean up very old items to prevent DB bloat
        // We keep items for 7 days regardless of the window
        val cleanupThreshold = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli()
        rssDao.deleteOldItems(cleanupThreshold)
        
        // 3. Get recent items within the requested window
        val since = Instant.now().minus(timeWindowHours.toLong(), ChronoUnit.HOURS).toEpochMilli()
        val recentItems = rssDao.getAllRecentItems(since)
        
        // 4. Match tickers in titles and snippets
        val digestMap = mutableMapOf<String, List<RssHeadline>>()
        
        tickers.forEach { ticker ->
            val matches = recentItems.filter { item ->
                isMatch(item.title, ticker) || isMatch(item.snippet, ticker)
            }
            .distinctBy { it.guidHash } // Avoid duplicates across feeds
            .sortedByDescending { it.publishedAt }
            .take(maxItemsPerTicker)
            .map { it.toHeadline() }
            
            if (matches.isNotEmpty()) {
                digestMap[ticker] = matches
            }
        }
        
        return RssDigest(digestMap)
    }

    private fun isMatch(text: String, ticker: String): Boolean {
        if (text.isBlank()) return false
        val safeTicker = Regex.escape(ticker.trim())
        if (safeTicker.isBlank()) return false
        // Match $TICKER or \bTICKER\b (word boundaries)
        // We use IGNORE_CASE to handle various mentions
        val cashtag = "\\$$safeTicker\\b".toRegex(RegexOption.IGNORE_CASE)
        val bareTicker = "\\b$safeTicker\\b".toRegex(RegexOption.IGNORE_CASE)
        return cashtag.containsMatchIn(text) || bareTicker.containsMatchIn(text)
    }

    private fun RssItemEntity.toHeadline(): RssHeadline {
        return RssHeadline(
            title = title,
            link = link,
            publishedAt = Instant.ofEpochMilli(publishedAt).toString(),
            snippet = snippet
        )
    }
}
