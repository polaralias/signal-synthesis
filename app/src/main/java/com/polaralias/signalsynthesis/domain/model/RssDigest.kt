package com.polaralias.signalsynthesis.domain.model

import org.json.JSONObject
import com.polaralias.signalsynthesis.util.Logger

data class RssDigest(
    val itemsBySymbol: Map<String, List<RssHeadline>> = emptyMap()
) {
    companion object {
        fun fromJson(json: String?): RssDigest {
            if (json == null) return RssDigest()
            return try {
                val obj = JSONObject(json)
                val itemsBySymbol = mutableMapOf<String, List<RssHeadline>>()
                obj.keys().forEach { symbol ->
                    val headlineArray = obj.optJSONArray(symbol)
                    val headlines = mutableListOf<RssHeadline>()
                    if (headlineArray != null) {
                        for (i in 0 until headlineArray.length()) {
                            val headObj = headlineArray.optJSONObject(i)
                            if (headObj != null) {
                                headlines.add(RssHeadline.fromJson(headObj))
                            }
                        }
                    }
                    itemsBySymbol[symbol] = headlines
                }
                RssDigest(itemsBySymbol)
            } catch (e: Exception) {
                Logger.e("RssDigest", "Failed to parse RSS digest JSON", e)
                RssDigest()
            }
        }
    }
}

data class RssHeadline(
    val title: String = "",
    val link: String = "",
    val publishedAt: String = "", // ISO string or similar
    val snippet: String = ""
) {
    companion object {
        fun fromJson(obj: JSONObject): RssHeadline {
            return RssHeadline(
                title = obj.optString("title", ""),
                link = obj.optString("link", ""),
                publishedAt = obj.optString("published_at", ""),
                snippet = obj.optString("snippet", "")
            )
        }
    }
}
