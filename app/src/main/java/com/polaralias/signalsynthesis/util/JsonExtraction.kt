package com.polaralias.signalsynthesis.util

import org.json.JSONArray
import org.json.JSONObject

object JsonExtraction {
    /**
     * Extracts the first JSON object string found in the text.
     * Returns the substring from the first '{' to the last '}'.
     */
    fun extractFirstJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }

    /**
     * Extension to safely convert JSONArray to List<String>.
     */
    fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val items = mutableListOf<String>()
        for (index in 0 until length()) {
            val value = optString(index)
            if (value != null) {
                items.add(value)
            }
        }
        return items.filter { it.isNotBlank() }
    }
}
