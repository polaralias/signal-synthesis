package com.polaralias.signalsynthesis.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.polaralias.signalsynthesis.util.Logger

class BlacklistStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blacklist_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(Map::class.java, String::class.java, Long::class.javaObjectType)
    private val adapter = moshi.adapter<Map<String, Long>>(type)

    fun saveBlacklist(blacklist: Map<String, Long>) {
        val json = adapter.toJson(blacklist)
        prefs.edit().putString("blacklist_data", json).apply()
    }

    fun loadBlacklist(): Map<String, Long> {
        val json = prefs.getString("blacklist_data", null) ?: return emptyMap()
        return try {
            adapter.fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            Logger.e("BlacklistStore", "Failed to parse blacklist", e)
            emptyMap()
        }
    }
}
