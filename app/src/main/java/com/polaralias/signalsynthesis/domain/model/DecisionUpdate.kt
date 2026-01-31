package com.polaralias.signalsynthesis.domain.model

import com.polaralias.signalsynthesis.util.JsonExtraction.toStringList
import org.json.JSONObject

data class DecisionUpdate(
    val keep: List<KeepItem> = emptyList(),
    val drop: List<DropItem> = emptyList(),
    val limitsApplied: Map<String, Int> = emptyMap()
) {
    companion object {
        fun fromJson(json: String?): DecisionUpdate {
            if (json == null) return DecisionUpdate()
            return try {
                val obj = JSONObject(json)
                
                val keepList = mutableListOf<KeepItem>()
                val keepArray = obj.optJSONArray("keep")
                if (keepArray != null) {
                    for (i in 0 until keepArray.length()) {
                        val itemObj = keepArray.optJSONObject(i)
                        if (itemObj != null) {
                            keepList.add(KeepItem.fromJson(itemObj))
                        }
                    }
                }

                val dropList = mutableListOf<DropItem>()
                val dropArray = obj.optJSONArray("drop")
                if (dropArray != null) {
                    for (i in 0 until dropArray.length()) {
                        val itemObj = dropArray.optJSONObject(i)
                        if (itemObj != null) {
                            dropList.add(DropItem.fromJson(itemObj))
                        }
                    }
                }

                val limitsObj = obj.optJSONObject("limits_applied")
                val limitsApplied = mutableMapOf<String, Int>()
                limitsObj?.keys()?.forEach { key ->
                    limitsApplied[key] = limitsObj.optInt(key)
                }

                DecisionUpdate(keepList, dropList, limitsApplied)
            } catch (e: Exception) {
                DecisionUpdate()
            }
        }
    }
}

data class KeepItem(
    val symbol: String = "",
    val confidence: Double = 0.0,
    val setupBias: String = "",
    val mustReview: List<String> = emptyList(),
    val rssNeeded: Boolean = false
) {
    companion object {
        fun fromJson(obj: JSONObject): KeepItem {
            return KeepItem(
                symbol = obj.optString("symbol", ""),
                confidence = obj.optDouble("confidence", 0.0),
                setupBias = obj.optString("setup_bias", ""),
                mustReview = obj.optJSONArray("must_review").toStringList(),
                rssNeeded = obj.optBoolean("rss_needed", false)
            )
        }
    }
}

data class DropItem(
    val symbol: String = "",
    val reasons: List<String> = emptyList()
) {
    companion object {
        fun fromJson(obj: JSONObject): DropItem {
            return DropItem(
                symbol = obj.optString("symbol", ""),
                reasons = obj.optJSONArray("reasons").toStringList()
            )
        }
    }
}
