package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.rss.RssDao
import com.polaralias.signalsynthesis.data.rss.RssFeedClient
import com.polaralias.signalsynthesis.data.rss.RssFeedStateEntity
import com.polaralias.signalsynthesis.data.rss.RssItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRssDigestUseCaseTest {

    @Test
    fun `match tickers correctly with cashtags and bare symbols`() = runTest {
        val now = System.currentTimeMillis()
        val items = listOf(
            RssItemEntity("h1", "url", "Buy ${"$"}AAPL now", "link", now - 10000, "snippet", now),
            RssItemEntity("h2", "url", "TSLA earnings report", "link", now - 20000, "snippet", now),
            RssItemEntity("h3", "url", "Microsoft (MSFT) update", "link", now - 30000, "snippet", now),
            RssItemEntity("h4", "url", "Random news", "link", now - 40000, "snippet", now)
        )
        
        val fakeDao = FakeRssDao(items)
        val fakeClient = FakeRssClient(fakeDao)
        val useCase = BuildRssDigestUseCase(fakeClient, fakeDao)
        
        val digest = useCase.execute(
            tickers = listOf("AAPL", "TSLA", "MSFT", "GOOG"),
            feedUrls = listOf("url"),
            timeWindowHours = 100000 // Huge window to include all
        )
        
        assertEquals(3, digest.itemsBySymbol.size)
        assertTrue(digest.itemsBySymbol.containsKey("AAPL"))
        assertTrue(digest.itemsBySymbol.containsKey("TSLA"))
        assertTrue(digest.itemsBySymbol.containsKey("MSFT"))
        
        assertEquals("Buy ${"$"}AAPL now", digest.itemsBySymbol["AAPL"]!![0].title)
    }

    @Test
    fun `only selected feeds contribute to digest`() = runTest {
        val now = System.currentTimeMillis()
        val items = listOf(
            RssItemEntity("selected", "selected-url", "AAPL from selected feed", "link", now - 1000, "snippet", now),
            RssItemEntity("other", "other-url", "AAPL from unrelated feed", "link", now - 500, "snippet", now)
        )

        val fakeDao = FakeRssDao(items)
        val useCase = BuildRssDigestUseCase(FakeRssClient(fakeDao), fakeDao)

        val digest = useCase.execute(
            tickers = listOf("AAPL"),
            feedUrls = listOf("selected-url"),
            timeWindowHours = 24
        )

        assertEquals(1, digest.itemsBySymbol["AAPL"]?.size)
        assertEquals("AAPL from selected feed", digest.itemsBySymbol["AAPL"]?.first()?.title)
    }

    class FakeRssDao(private var items: List<RssItemEntity>) : RssDao {
        override suspend fun getFeedState(url: String): RssFeedStateEntity? = null
        override suspend fun insertFeedState(state: RssFeedStateEntity) {}
        override suspend fun getAllRecentItems(since: Long): List<RssItemEntity> = items.filter { it.publishedAt >= since }
        override suspend fun getRecentItemsForFeeds(feedUrls: List<String>, since: Long): List<RssItemEntity> =
            items.filter { it.feedUrl in feedUrls && it.publishedAt >= since }
        override suspend fun getItemsForFeed(url: String, limit: Int): List<RssItemEntity> =
            items.filter { it.feedUrl == url }.sortedByDescending { it.publishedAt }.take(limit)
        override suspend fun insertItems(items: List<RssItemEntity>) {}
        override suspend fun deleteOldItems(threshold: Long) {
            items = items.filter { it.publishedAt >= threshold }
        }
    }

    class FakeRssClient(dao: RssDao) : RssFeedClient(okhttp3.OkHttpClient(), dao) {
        override suspend fun fetchFeed(url: String) {
            // Do nothing
        }
    }
}
