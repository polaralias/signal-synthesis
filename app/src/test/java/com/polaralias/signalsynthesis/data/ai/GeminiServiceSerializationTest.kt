package com.polaralias.signalsynthesis.data.ai

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiServiceSerializationTest {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(GeminiRequest::class.java)

    @Test
    fun serializesThinkingLevelUnderThinkingConfig() {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = "test prompt")))
            ),
            generationConfig = GeminiGenerationConfig(
                thinkingConfig = GeminiThinkingConfig(thinkingLevel = "high")
            )
        )

        val json = adapter.toJson(request)

        assertTrue(json.contains("\"thinkingConfig\":{\"thinkingLevel\":\"high\"}"))
        assertFalse(json.contains("thinking_level"))
    }

    @Test
    fun serializesThinkingBudgetUnderThinkingConfig() {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = "test prompt")))
            ),
            generationConfig = GeminiGenerationConfig(
                thinkingConfig = GeminiThinkingConfig(thinkingBudget = 500)
            )
        )

        val json = adapter.toJson(request)

        assertTrue(json.contains("\"thinkingConfig\":{\"thinkingBudget\":500}"))
        assertFalse(json.contains("thinking_budget"))
    }
}
