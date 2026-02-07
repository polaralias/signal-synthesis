package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.model.DeepDiveSource
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeminiDeepDiveProviderTest {

    @Test
    fun testGroundingMetadataMapping() = runBlocking {
        // Create a mock service that returns a response with grounding metadata
        val mockService = object : GeminiService {
            override suspend fun generateContent(apiVersion: String, model: String, apiKey: String, request: GeminiRequest): GeminiResponse {
                return GeminiResponse(
                    candidates = listOf(
                        GeminiCandidate(
                            content = GeminiContent(
                                parts = listOf(GeminiPart(text = "{\"summary\": \"Test\"}"))
                            ),
                            groundingMetadata = GeminiGroundingMetadata(
                                groundingChunks = listOf(
                                    GeminiGroundingChunk(
                                        web = GeminiWebGroundingChunk(
                                            uri = "https://example.com/news",
                                            title = "Example News"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }
        }

        val provider = GeminiDeepDiveProvider(mockService)
        val result = provider.deepDive(
            symbol = "AAPL",
            intent = TradingIntent.SWING,
            snapshot = "Test snapshot",
            rssHeadlinesList = emptyList(),
            apiKey = "key",
            model = "gemini-3-pro",
            timeoutMs = 5000
        )

        assertEquals(1, result.sources.size)
        assertEquals("Example News", result.sources[0].title)
        assertEquals("https://example.com/news", result.sources[0].url)
    }

    @Test
    fun testJsonExtractionInProvider() = runBlocking {
        val mockService = object : GeminiService {
            override suspend fun generateContent(apiVersion: String, model: String, apiKey: String, request: GeminiRequest): GeminiResponse {
                return GeminiResponse(
                    candidates = listOf(
                        GeminiCandidate(
                            content = GeminiContent(
                                parts = listOf(GeminiPart(text = "Here is the result: {\"summary\": \"Success\"}"))
                            )
                        )
                    )
                )
            }
        }

        val provider = GeminiDeepDiveProvider(mockService)
        val result = provider.deepDive(
            symbol = "AAPL",
            intent = TradingIntent.SWING,
            snapshot = "Test snapshot",
            rssHeadlinesList = emptyList(),
            apiKey = "key",
            model = "gemini-3-pro",
            timeoutMs = 5000
        )

        assertEquals("Success", result.summary)
    }
}
