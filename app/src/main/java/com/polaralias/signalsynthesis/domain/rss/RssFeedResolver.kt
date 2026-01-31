package com.polaralias.signalsynthesis.domain.rss

import com.polaralias.signalsynthesis.data.rss.RssFeedCatalog
import com.polaralias.signalsynthesis.data.rss.RssFeedDefaults
import com.polaralias.signalsynthesis.domain.model.TickerSource

data class RssFeedSelection(
    val enabledTopicKeys: Set<String>,
    val enabledTickerSourceIds: Set<String>,
    val useTickerFeedsForFinalStage: Boolean,
    val forceExpandedForAll: Boolean
)

data class RssTickerInput(
    val symbol: String,
    val source: TickerSource,
    val rssNeeded: Boolean,
    val expandedRssNeeded: Boolean
)

enum class RssFeedStage { ANALYSIS, DEEP_DIVE }

data class RssFeedResolution(
    val feedUrls: List<String>,
    val expandedApplied: Boolean
)

class RssFeedResolver {
    fun resolve(
        catalog: RssFeedCatalog,
        selection: RssFeedSelection,
        tickers: List<RssTickerInput>,
        stage: RssFeedStage,
        maxTickerSourcesPerSymbol: Int = 2,
        maxExpandedTopics: Int = 6
    ): RssFeedResolution {
        val enabledTopics = selection.enabledTopicKeys

        val expandedKeys = enabledTopics.intersect(RssFeedDefaults.expandedTopicKeys)
        val coreKeys = enabledTopics - expandedKeys

        val expandedNeeded = when (stage) {
            RssFeedStage.ANALYSIS -> tickers.any { it.expandedRssNeeded }
            RssFeedStage.DEEP_DIVE -> selection.forceExpandedForAll || tickers.any { it.expandedRssNeeded }
        }

        val expandedKeysLimited = if (expandedNeeded) {
            val prioritized = RssFeedDefaults.expandedTopicPriority.filter { expandedKeys.contains(it) }
            val remaining = expandedKeys.filterNot { RssFeedDefaults.expandedTopicPriority.contains(it) }
            (prioritized + remaining).take(maxExpandedTopics).toSet()
        } else {
            emptySet()
        }

        val coreUrls = coreKeys.mapNotNull { key ->
            catalog.entryByKey(key)
                ?.takeIf { !it.isTickerTemplate }
                ?.url
        }

        val expandedUrls = expandedKeysLimited.mapNotNull { key ->
            catalog.entryByKey(key)
                ?.takeIf { !it.isTickerTemplate }
                ?.url
        }

        val tickerTemplates = catalog.entries
            .filter { it.isTickerTemplate && selection.enabledTickerSourceIds.contains(it.sourceId) }
            .sortedBy { template ->
                val priority = RssFeedDefaults.tickerSourcePriority.indexOf(template.sourceId)
                if (priority == -1) Int.MAX_VALUE else priority
            }

        val tickerUrls = buildList {
            tickers.forEach { ticker ->
                val includeTickerFeeds = when (stage) {
                    RssFeedStage.ANALYSIS -> ticker.source == TickerSource.CUSTOM ||
                        (selection.useTickerFeedsForFinalStage && ticker.rssNeeded)
                    RssFeedStage.DEEP_DIVE -> ticker.source == TickerSource.CUSTOM ||
                        selection.useTickerFeedsForFinalStage
                }

                if (includeTickerFeeds) {
                    tickerTemplates.take(maxTickerSourcesPerSymbol).forEach { template ->
                        add(applyTickerTemplate(template.url, ticker.symbol))
                    }
                }
            }
        }

        val feedUrls = (coreUrls + expandedUrls + tickerUrls).distinct()
        return RssFeedResolution(feedUrls = feedUrls, expandedApplied = expandedNeeded)
    }

    private fun applyTickerTemplate(url: String, symbol: String): String {
        val normalized = symbol.trim().uppercase()
        return when {
            url.contains("{}") -> url.replace("{}", normalized)
            url.contains("{symbol}") -> url.replace("{symbol}", normalized)
            else -> url
        }
    }
}
