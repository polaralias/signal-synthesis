package com.polaralias.signalsynthesis.data.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimedCacheTest {

    @Test
    fun getReturnsNullForMissingKey() {
        val cache = TimedCache<String, String>(1000)
        assertNull(cache.get("missing"))
    }

    @Test
    fun putThenGetReturnsValue() {
        val cache = TimedCache<String, String>(1000)
        cache.put("key", "value")
        assertEquals("value", cache.get("key"))
    }

    @Test
    fun getReturnsNullAfterExpiration() {
        var currentTime = 1000L
        val clock = { currentTime }
        val cache = TimedCache<String, String>(ttlMillis = 100, clockMillis = clock)

        cache.put("key", "value")
        assertEquals("value", cache.get("key"))

        // Advance time beyond TTL
        currentTime += 101
        assertNull(cache.get("key"))
    }

    @Test
    fun clearRemovesAllEntries() {
        val cache = TimedCache<String, String>(1000)
        cache.put("key1", "value1")
        cache.put("key2", "value2")

        assertEquals(2, cache.size())

        cache.clear()

        assertEquals(0, cache.size())
        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
    }

    @Test
    fun removeDeletesSpecificKey() {
        val cache = TimedCache<String, String>(1000)
        cache.put("key1", "value1")
        cache.put("key2", "value2")

        cache.remove("key1")

        assertNull(cache.get("key1"))
        assertEquals("value2", cache.get("key2"))
    }
}
