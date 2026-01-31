package com.polaralias.signalsynthesis.domain.rss

import com.polaralias.signalsynthesis.data.rss.RssFeedCatalog
import com.polaralias.signalsynthesis.data.rss.RssFeedCatalogEntry
import com.polaralias.signalsynthesis.data.rss.RssFeedDefaults
import com.polaralias.signalsynthesis.domain.model.TickerSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RssFeedResolverTest {
    private val catalog = RssFeedCatalog(
        entries = listOf(
            RssFeedCatalogEntry("reuters", "Reuters", "top_news", "top news", "http://reuters/top", false),
            RssFeedCatalogEntry("yahoo_finance", "Yahoo Finance", "top_stories", "top stories", "http://yahoo/top", false),
            RssFeedCatalogEntry("seeking_alpha", "Seeking Alpha", "all_news", "all news", "http://sa/all", false),
            RssFeedCatalogEntry("yahoo_finance", "Yahoo Finance", "ticker", "ticker", "http://yahoo/{symbol}", true),
            RssFeedCatalogEntry("seeking_alpha", "Seeking Alpha", "ticker", "ticker", "http://sa/{}", true)
        )
    )

    @Test
    fun `analysis uses core feeds and ticker templates when rss needed`() {
        val resolver = RssFeedResolver()
        val selection = RssFeedSelection(
            enabledTopicKeys = RssFeedDefaults.coreTopicKeys + RssFeedDefaults.expandedTopicKeys,
            enabledTickerSourceIds = setOf("yahoo_finance", "seeking_alpha"),
            useTickerFeedsForFinalStage = true,
            forceExpandedForAll = false
        )

        val resolution = resolver.resolve(
            catalog = catalog,
            selection = selection,
            tickers = listOf(
                RssTickerInput("AAPL", TickerSource.PREDEFINED, rssNeeded = true, expandedRssNeeded = false)
            ),
            stage = RssFeedStage.ANALYSIS,
            maxTickerSourcesPerSymbol = 1,
            maxExpandedTopics = 3
        )

        assertTrue(resolution.feedUrls.contains("http://reuters/top"))
        assertTrue(resolution.feedUrls.contains("http://yahoo/top"))
        assertTrue(resolution.feedUrls.any { it.contains("AAPL") })
        assertFalse(resolution.feedUrls.contains("http://sa/all"))
    }

    @Test
    fun `analysis includes expanded feeds when expanded needed`() {
        val resolver = RssFeedResolver()
        val selection = RssFeedSelection(
            enabledTopicKeys = RssFeedDefaults.coreTopicKeys + RssFeedDefaults.expandedTopicKeys,
            enabledTickerSourceIds = setOf("yahoo_finance"),
            useTickerFeedsForFinalStage = true,
            forceExpandedForAll = false
        )

        val resolution = resolver.resolve(
            catalog = catalog,
            selection = selection,
            tickers = listOf(
                RssTickerInput("TSLA", TickerSource.PREDEFINED, rssNeeded = true, expandedRssNeeded = true)
            ),
            stage = RssFeedStage.ANALYSIS
        )

        assertTrue(resolution.feedUrls.contains("http://sa/all"))
    }

    @Test
    fun `deep dive can force expanded feeds for all`() {
        val resolver = RssFeedResolver()
        val selection = RssFeedSelection(
            enabledTopicKeys = RssFeedDefaults.coreTopicKeys + RssFeedDefaults.expandedTopicKeys,
            enabledTickerSourceIds = setOf("yahoo_finance"),
            useTickerFeedsForFinalStage = true,
            forceExpandedForAll = true
        )

        val resolution = resolver.resolve(
            catalog = catalog,
            selection = selection,
            tickers = listOf(
                RssTickerInput("NVDA", TickerSource.PREDEFINED, rssNeeded = false, expandedRssNeeded = false)
            ),
            stage = RssFeedStage.DEEP_DIVE
        )

        assertTrue(resolution.feedUrls.contains("http://sa/all"))
    }
}
