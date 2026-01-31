package com.polaralias.signalsynthesis.domain.model

import com.polaralias.signalsynthesis.util.JsonExtraction
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LlmModelParsingTest {

    @Test
    fun testJsonExtractionSurroundedByText() {
        val text = "Setting things up... \n {\"key\": \"value\"} \n Done."
        val extracted = JsonExtraction.extractFirstJsonObject(text)
        assertEquals("{\"key\": \"value\"}", extracted)
    }

    @Test
    fun testShortlistPlanParsing() {
        val json = """
            {
              "shortlist": [
                {
                  "symbol": "AAPL",
                  "priority": 0.92,
                  "reasons": ["liquid", "trend_strength"],
                  "requested_enrichment": ["INTRADAY"],
                  "avoid": false,
                  "risk_flags": ["earnings_soon"]
                }
              ],
              "global_notes": ["Note 1"],
              "limits_applied": { "max_shortlist": 15 }
            }
        """.trimIndent()
        
        val plan = ShortlistPlan.fromJson(json)
        assertEquals(1, plan.shortlist.size)
        assertEquals("AAPL", plan.shortlist[0].symbol)
        assertEquals(0.92, plan.shortlist[0].priority, 0.001)
        assertEquals("Note 1", plan.globalNotes[0])
        assertEquals(15, plan.limitsApplied["max_shortlist"])
    }

    @Test
    fun testShortlistPlanMissingFields() {
        val json = "{ \"shortlist\": [] }"
        val plan = ShortlistPlan.fromJson(json)
        assertTrue(plan.shortlist.isEmpty())
        assertTrue(plan.globalNotes.isEmpty())
        assertTrue(plan.limitsApplied.isEmpty())
    }

    @Test
    fun testDecisionUpdateParsing() {
        val json = """
            {
              "keep": [
                {
                  "symbol": "AAPL",
                  "confidence": 0.81,
                  "setup_bias": "bullish",
                  "must_review": ["invalidations"],
                  "rss_needed": true
                }
              ],
              "drop": [
                { "symbol": "XYZ", "reasons": ["illiquid"] }
              ],
              "limits_applied": { "max_keep": 10 }
            }
        """.trimIndent()

        val update = DecisionUpdate.fromJson(json)
        assertEquals(1, update.keep.size)
        assertEquals("AAPL", update.keep[0].symbol)
        assertTrue(update.keep[0].rssNeeded)
        assertEquals(1, update.drop.size)
        assertEquals("XYZ", update.drop[0].symbol)
        assertEquals(10, update.limitsApplied["max_keep"])
    }

    @Test
    fun testRssDigestParsing() {
        val json = """
            {
              "AAPL": [
                {
                  "title": "Title 1",
                  "link": "https://link1.com",
                  "published_at": "2023-01-01",
                  "snippet": "Snippet 1"
                }
              ]
            }
        """.trimIndent()

        val digest = RssDigest.fromJson(json)
        assertTrue(digest.itemsBySymbol.containsKey("AAPL"))
        assertEquals(1, digest.itemsBySymbol["AAPL"]?.size)
        assertEquals("Title 1", digest.itemsBySymbol["AAPL"]?.get(0)?.title)
    }

    @Test
    fun testFundamentalsNewsSynthesisParsing() {
        val json = """
            {
              "ranked_review_list": [
                {
                  "symbol": "AAPL",
                  "what_to_review": ["News"],
                  "risk_summary": ["Gap"],
                  "one_paragraph_brief": "Briefly..."
                }
              ],
              "portfolio_guidance": {
                "position_count": 3,
                "risk_posture": "moderate"
              }
            }
        """.trimIndent()

        val synthesis = FundamentalsNewsSynthesis.fromJson(json)
        assertEquals(1, synthesis.rankedReviewList.size)
        assertEquals("AAPL", synthesis.rankedReviewList[0].symbol)
        assertEquals(3, synthesis.portfolioGuidance.positionCount)
        assertEquals("moderate", synthesis.portfolioGuidance.riskPosture)
    }

    @Test
    fun testMalformedJsonDoesNotCrash() {
        val malformed = "{ \"shortlist\": [ { \"symbol\": \"AAPL\" " // missing closing braces
        
        // ShortlistPlan.fromJson should handle JSONException internally
        val plan = ShortlistPlan.fromJson(malformed)
        assertTrue(plan.shortlist.isEmpty())
        
        val update = DecisionUpdate.fromJson("not a json")
        assertTrue(update.keep.isEmpty())
    }
}
