package com.polaralias.signalsynthesis.data.provider

import android.content.Context
import com.polaralias.signalsynthesis.data.storage.BlacklistStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

object ProviderStatusManager {
    private val _blacklist = MutableStateFlow<Map<String, Long>>(emptyMap())
    val blacklist: StateFlow<Map<String, Long>> = _blacklist.asStateFlow()

    private val blacklistInternal = ConcurrentHashMap<String, Long>()
    private var blacklistStore: BlacklistStore? = null

    var onBlacklisted: ((String) -> Unit)? = null

    fun initialize(context: Context) {
        if (blacklistStore != null) return
        val store = BlacklistStore(context)
        blacklistStore = store
        
        // Load existing blacklist and filter expired items
        val loaded = store.loadBlacklist()
        val now = System.currentTimeMillis()
        loaded.forEach { (name, until) ->
            if (until > now) {
                blacklistInternal[name] = until
            }
        }
        updateFlow()
    }

    fun blacklistProvider(providerName: String, durationMs: Long) {
        val until = System.currentTimeMillis() + durationMs
        blacklistInternal[providerName] = until
        saveAndUpdate()
        onBlacklisted?.invoke(providerName)
    }

    fun isBlacklisted(providerName: String): Boolean {
        val until = blacklistInternal[providerName] ?: return false
        if (System.currentTimeMillis() > until) {
            blacklistInternal.remove(providerName)
            saveAndUpdate()
            return false
        }
        return true
    }

    fun getBlacklistedProviders(): List<String> {
        val now = System.currentTimeMillis()
        val expired = blacklistInternal.filterValues { it <= now }.keys
        if (expired.isNotEmpty()) {
            expired.forEach { blacklistInternal.remove(it) }
            saveAndUpdate()
        }
        return blacklistInternal.keys().toList()
    }

    private fun saveAndUpdate() {
        blacklistStore?.saveBlacklist(blacklistInternal.toMap())
        updateFlow()
    }

    private fun updateFlow() {
        _blacklist.update { blacklistInternal.toMap() }
    }
}
