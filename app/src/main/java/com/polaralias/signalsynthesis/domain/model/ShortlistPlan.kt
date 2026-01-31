package com.polaralias.signalsynthesis.domain.model

import com.polaralias.signalsynthesis.util.JsonExtraction
import com.polaralias.signalsynthesis.util.JsonExtraction.toStringList
import org.json.JSONObject

data class ShortlistPlan(
    val shortlist: List<ShortlistItem> = emptyList(),
    val globalNotes: List<String> = emptyList(),
    val limitsApplied: Map<String, Int> = emptyMap()
) {
    companion object {
        fun fromJson(json: String?): ShortlistPlan {
            if (json == null) return ShortlistPlan()
            return try {
                val obj = JSONObject(json)
                val shortlistArray = obj.optJSONArray("shortlist")
                val shortlist = mutableListOf<ShortlistItem>()
                if (shortlistArray != null) {
                    for (i in 0 until shortlistArray.length()) {
                        val itemObj = shortlistArray.optJSONObject(i)
                        if (itemObj != null) {
                            shortlist.add(ShortlistItem.fromJson(itemObj))
                        }
                    }
                }

                val globalNotes = obj.optJSONArray("global_notes").toStringList()
                
                val limitsObj = obj.optJSONObject("limits_applied")
                val limitsApplied = mutableMapOf<String, Int>()
                limitsObj?.keys()?.forEach { key ->
                    limitsApplied[key] = limitsObj.optInt(key)
                }

                ShortlistPlan(shortlist, globalNotes, limitsApplied)
            } catch (e: Exception) {
                ShortlistPlan()
            }
        }
    }
}

data class ShortlistItem(
    val symbol: String = "",
    val priority: Double = 0.0,
    val reasons: List<String> = emptyList(),
    val requestedEnrichment: List<String> = emptyList(),
    val avoid: Boolean = false,
    val riskFlags: List<String> = emptyList()
) {
    companion object {
        fun fromJson(obj: JSONObject): ShortlistItem {
            return ShortlistItem(
                symbol = obj.optString("symbol", ""),
                priority = obj.optDouble("priority", 0.0),
                reasons = obj.optJSONArray("reasons").toStringList(),
                requestedEnrichment = obj.optJSONArray("requested_enrichment").toStringList(),
                avoid = obj.optBoolean("avoid", false),
                riskFlags = obj.optJSONArray("risk_flags").toStringList()
            )
        }
    }
}
