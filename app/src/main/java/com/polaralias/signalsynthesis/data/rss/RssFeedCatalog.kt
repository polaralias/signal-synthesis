package com.polaralias.signalsynthesis.data.rss

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class RssFeedCatalogEntry(
    val sourceId: String,
    val sourceLabel: String,
    val topicId: String,
    val topicLabel: String,
    val url: String,
    val isTickerTemplate: Boolean = false
) {
    val topicKey: String get() = "${sourceId}:${topicId}"
}

data class RssFeedTopic(
    val id: String,
    val label: String,
    val url: String,
    val isTickerTemplate: Boolean
)

data class RssFeedSource(
    val id: String,
    val label: String,
    val topics: List<RssFeedTopic>
)

class RssFeedCatalog(val entries: List<RssFeedCatalogEntry>) {
    val sources: List<RssFeedSource> = entries
        .groupBy { it.sourceId }
        .map { (sourceId, items) ->
            val label = items.firstOrNull()?.sourceLabel ?: sourceId
            val topics = items
                .map { entry ->
                    RssFeedTopic(
                        id = entry.topicId,
                        label = entry.topicLabel,
                        url = entry.url,
                        isTickerTemplate = entry.isTickerTemplate
                    )
                }
                .sortedBy { it.label.lowercase() }
            RssFeedSource(sourceId, label, topics)
        }
        .sortedBy { it.label.lowercase() }

    fun entryByKey(key: String): RssFeedCatalogEntry? = entries.firstOrNull { it.topicKey == key }
}

class RssFeedCatalogLoader(private val context: Context) {
    private var cached: RssFeedCatalog? = null

    fun load(): RssFeedCatalog {
        cached?.let { return it }
        return try {
            val json = context.assets.open("rss_feeds.json").bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, RssFeedCatalogEntry::class.java)
            val adapter = moshi.adapter<List<RssFeedCatalogEntry>>(type)
            val entries = adapter.fromJson(json).orEmpty()
            val catalog = RssFeedCatalog(entries)
            cached = catalog
            catalog
        } catch (_: Exception) {
            val catalog = RssFeedCatalog(emptyList())
            cached = catalog
            catalog
        }
    }
}
