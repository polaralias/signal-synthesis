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

    @Test
    fun geminiAliasesResolveToCurrentPreviewIds() {
        assertEquals("gemini-3.1-pro-preview", LlmModel.normalizeModelIdAlias("gemini-3-pro"))
        assertEquals("gemini-3-flash-preview", LlmModel.normalizeModelIdAlias("gemini-3-flash"))
    }

    @Test
    fun deprecatedProviderAliasesResolveToCurrentModelIds() {
        assertEquals("MiniMax-M2.5", LlmModel.normalizeModelIdAlias("M2"))
        assertEquals("MiniMax-M2.5-highspeed", LlmModel.normalizeModelIdAlias("M2-Pro"))
        assertEquals("llama-3.1-8b-instant", LlmModel.normalizeModelIdAlias("mixtral-8x7b-32768"))
    }

    @Test
    fun openAiReasoningEffortAvoidsUnsupportedMinimalOnModernGpt5Variants() {
        assertEquals("none", LlmModel.openAiReasoningEffort("gpt-5.1", ReasoningDepth.NONE))
        assertEquals("low", LlmModel.openAiReasoningEffort("gpt-5.1", ReasoningDepth.MINIMAL))
        assertEquals("none", LlmModel.openAiReasoningEffort("gpt-5.2", ReasoningDepth.NONE))
        assertEquals("high", LlmModel.openAiReasoningEffort("gpt-5.2", ReasoningDepth.EXTRA))
    }
}
