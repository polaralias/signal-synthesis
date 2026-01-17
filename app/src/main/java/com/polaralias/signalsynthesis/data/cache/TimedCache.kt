package com.polaralias.signalsynthesis.data.cache

class TimedCache<K, V>(
    private val ttlMillis: Long,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private data class Entry<V>(val value: V, val timestampMillis: Long)

    private val entries = mutableMapOf<K, Entry<V>>()

    fun get(key: K): V? {
        synchronized(entries) {
            val entry = entries[key] ?: return null
            val age = clockMillis() - entry.timestampMillis
            if (age > ttlMillis) {
                entries.remove(key)
                return null
            }
            return entry.value
        }
    }

    fun put(key: K, value: V) {
        synchronized(entries) {
            entries[key] = Entry(value, clockMillis())
        }
    }

    fun remove(key: K) {
        synchronized(entries) {
            entries.remove(key)
        }
    }

    fun clear() {
        synchronized(entries) {
            entries.clear()
        }
    }

    fun size(): Int {
        synchronized(entries) {
            return entries.size
        }
    }
}
