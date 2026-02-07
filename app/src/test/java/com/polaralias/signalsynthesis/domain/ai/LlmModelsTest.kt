package com.polaralias.signalsynthesis.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmModelsTest {

    @Test
    fun openAiProviderDefaultsToResponsesApi() {
        assertEquals(LlmApiFormat.OPENAI_RESPONSES, LlmProvider.OPENAI.apiFormat)
    }

    @Test
    fun openAiFamiliesResolveToResponsesApi() {
        val openAiModelIds = listOf(
            "gpt-5.2",
            "o3",
            "gpt-4o",
            "chatgpt-4o-latest"
        )

        openAiModelIds.forEach { modelId ->
            assertTrue("Expected $modelId to use OpenAI Responses API", LlmModel.usesOpenAiResponsesApi(modelId))
        }
    }

    @Test
    fun nonOpenAiFamiliesDoNotResolveToResponsesApi() {
        assertFalse(LlmModel.usesOpenAiResponsesApi("claude-sonnet-4-5"))
        assertFalse(LlmModel.usesOpenAiResponsesApi("gemini-2.5-pro"))
    }
}
