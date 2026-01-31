package com.polaralias.signalsynthesis.data.rss

enum class RssFeedTier { CORE, EXPANDED, OTHER }

object RssFeedDefaults {
    fun topicKey(sourceId: String, topicId: String): String = "$sourceId:$topicId"

    val coreTopicKeys: Set<String> = setOf(
        topicKey("reuters", "top_news"),
        topicKey("cnbc", "top_news"),
        topicKey("cnbc", "business"),
        topicKey("cnbc", "earnings"),
        topicKey("marketwatch", "top_stories"),
        topicKey("yahoo_finance", "top_stories")
    )

    val expandedTopicKeys: Set<String> = setOf(
        topicKey("seeking_alpha", "all_news"),
        topicKey("nasdaq", "markets"),
        topicKey("wsj", "markets_news"),
        topicKey("ft", "news"),
        topicKey("fortune", "breaking_business_news"),
        topicKey("zacks", "all_commentary_articles"),
        topicKey("cnn_money", "top_stories")
    )

    val expandedTopicPriority: List<String> = listOf(
        topicKey("seeking_alpha", "all_news"),
        topicKey("nasdaq", "markets"),
        topicKey("wsj", "markets_news"),
        topicKey("ft", "news"),
        topicKey("fortune", "breaking_business_news"),
        topicKey("zacks", "all_commentary_articles"),
        topicKey("cnn_money", "top_stories")
    )

    val defaultTickerSourceIds: Set<String> = setOf(
        "yahoo_finance",
        "seeking_alpha"
    )

    val tickerSourcePriority: List<String> = listOf(
        "yahoo_finance",
        "seeking_alpha",
        "nasdaq",
        "reddit"
    )

    fun defaultEnabledTopicKeys(): Set<String> = coreTopicKeys + expandedTopicKeys

    fun tierFor(key: String): RssFeedTier = when {
        coreTopicKeys.contains(key) -> RssFeedTier.CORE
        expandedTopicKeys.contains(key) -> RssFeedTier.EXPANDED
        else -> RssFeedTier.OTHER
    }
}
