package com.polaralias.signalsynthesis.domain.model

import com.polaralias.signalsynthesis.util.JsonExtraction.toStringList
import com.polaralias.signalsynthesis.util.Logger
import org.json.JSONObject

data class DeepDive(
    val summary: String = "",
    val drivers: List<DeepDiveDriver> = emptyList(),
    val risks: List<String> = emptyList(),
    val whatChangesMyMind: List<String> = emptyList(),
    val sources: List<DeepDiveSource> = emptyList()
) {
    companion object {
        fun fromJson(json: String?): DeepDive {
            if (json == null) return DeepDive()
            return try {
                val obj = JSONObject(json)
                
                val driversList = mutableListOf<DeepDiveDriver>()
                val driversArray = obj.optJSONArray("drivers")
                if (driversArray != null) {
                    for (i in 0 until driversArray.length()) {
                        val itemObj = driversArray.optJSONObject(i)
                        if (itemObj != null) {
                            driversList.add(DeepDiveDriver.fromJson(itemObj))
                        }
                    }
                }

                val sourcesList = mutableListOf<DeepDiveSource>()
                val sourcesArray = obj.optJSONArray("sources")
                if (sourcesArray != null) {
                    for (i in 0 until sourcesArray.length()) {
                        val itemObj = sourcesArray.optJSONObject(i)
                        if (itemObj != null) {
                            sourcesList.add(DeepDiveSource.fromJson(itemObj))
                        }
                    }
                }

                DeepDive(
                    summary = obj.optString("summary", ""),
                    drivers = driversList,
                    risks = obj.optJSONArray("risks").toStringList(),
                    whatChangesMyMind = obj.optJSONArray("what_changes_my_mind").toStringList(),
                    sources = sourcesList
                )
            } catch (e: Exception) {
                Logger.e("DeepDive", "Failed to parse deep dive JSON", e)
                DeepDive()
            }
        }
    }
}

data class DeepDiveDriver(
    val type: String = "",
    val direction: String = "",
    val detail: String = ""
) {
    companion object {
        fun fromJson(obj: JSONObject): DeepDiveDriver {
            return DeepDiveDriver(
                type = obj.optString("type", ""),
                direction = obj.optString("direction", ""),
                detail = obj.optString("detail", "")
            )
        }
    }
}

data class DeepDiveSource(
    val title: String = "",
    val publisher: String = "",
    val publishedAt: String = "",
    val url: String = ""
) {
    companion object {
        fun fromJson(obj: JSONObject): DeepDiveSource {
            return DeepDiveSource(
                title = obj.optString("title", ""),
                publisher = obj.optString("publisher", ""),
                publishedAt = obj.optString("published_at", ""),
                url = obj.optString("url", "")
            )
        }
    }
}
