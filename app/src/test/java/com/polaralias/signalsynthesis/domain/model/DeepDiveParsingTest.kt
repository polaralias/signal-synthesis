package com.polaralias.signalsynthesis.domain.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepDiveParsingTest {

    @Test
    fun testDeepDiveParsing() {
        val json = """
            {
              "summary": "Everything is great.",
              "drivers": [
                { "type": "technical", "direction": "bullish", "detail": "RSI oversold" }
              ],
              "risks": ["Earnings soon"],
              "what_changes_my_mind": ["Breaking below 150"],
              "sources": [
                { "title": "Source 1", "publisher": "Pub 1", "url": "https://link.com", "published_at": "2023-01-01" }
              ]
            }
        """.trimIndent()
        
        val deepDive = DeepDive.fromJson(json)
        assertEquals("Everything is great.", deepDive.summary)
        assertEquals(1, deepDive.drivers.size)
        assertEquals("technical", deepDive.drivers[0].type)
        assertEquals("bullish", deepDive.drivers[0].direction)
        assertEquals(1, deepDive.risks.size)
        assertEquals("Earnings soon", deepDive.risks[0])
        assertEquals("Breaking below 150", deepDive.whatChangesMyMind[0])
        assertEquals(1, deepDive.sources.size)
        assertEquals("Source 1", deepDive.sources[0].title)
        assertEquals("https://link.com", deepDive.sources[0].url)
    }

    @Test
    fun testDeepDiveEmptyJson() {
        val deepDive = DeepDive.fromJson("{}")
        assertEquals("", deepDive.summary)
        assertTrue(deepDive.drivers.isEmpty())
        assertTrue(deepDive.risks.isEmpty())
        assertTrue(deepDive.sources.isEmpty())
    }

    @Test
    fun testDeepDiveMalformedJson() {
        val deepDive = DeepDive.fromJson("invalid")
        assertEquals("", deepDive.summary)
        assertTrue(deepDive.drivers.isEmpty())
    }
}
