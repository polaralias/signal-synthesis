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
        assertEquals("gemini-3-pro-preview", LlmModel.normalizeModelIdAlias("gemini-3-pro"))
        assertEquals("gemini-3-flash-preview", LlmModel.normalizeModelIdAlias("gemini-3-flash"))
        assertEquals("gemini-3-pro-preview", LlmModel.normalizeModelIdAlias("models/gemini-3-pro-preview"))
        assertEquals("gemini-3.5-flash", LlmModel.normalizeModelIdAlias("models/gemini-3.5-flash"))
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

    @Test
    fun availableModelsForProviderPreferDiscoveredCurrentFrontierModels() {
        val openAiModels = LlmModel.availableModelsForProvider(
            LlmProvider.OPENAI,
            setOf("gpt-5.4", "gpt-5.4-mini", "gpt-5.5", "gpt-5.2")
        )
        assertEquals(
            listOf(LlmModel.GPT_5_4, LlmModel.GPT_5_4_MINI, LlmModel.GPT_5_5, LlmModel.GPT_5_2),
            openAiModels
        )

        val geminiModels = LlmModel.availableModelsForProvider(
            LlmProvider.GEMINI,
            setOf("models/gemini-3-pro-preview", "gemini-3.5-flash", "gemini-3-flash-preview", "gemini-2.5-pro")
        )
        assertEquals(
            listOf(LlmModel.GEMINI_3_PRO, LlmModel.GEMINI_3_5_FLASH, LlmModel.GEMINI_3_FLASH, LlmModel.GEMINI_2_5_PRO),
            geminiModels
        )
    }

    @Test
    fun recommendationTierUsesGenerationAndTierTogether() {
        assertEquals(ModelRecommendationTier.DEFAULT, LlmModel.recommendationTier(LlmModel.GPT_5_4))
        assertEquals(ModelRecommendationTier.CHEAPER, LlmModel.recommendationTier(LlmModel.GPT_5_4_MINI))
        assertEquals(ModelRecommendationTier.CHEAPEST, LlmModel.recommendationTier(LlmModel.GPT_5_4_NANO))
        assertEquals(ModelRecommendationTier.PREMIUM, LlmModel.recommendationTier(LlmModel.GPT_5_5))
        assertEquals(ModelRecommendationTier.PREMIUM, LlmModel.recommendationTier(LlmModel.CLAUDE_OPUS_4_6))
        assertEquals(ModelRecommendationTier.DEFAULT, LlmModel.recommendationTier(LlmModel.CLAUDE_SONNET_4_5))
        assertEquals(ModelRecommendationTier.CHEAPER, LlmModel.recommendationTier(LlmModel.CLAUDE_HAIKU_4_5))
        assertEquals(ModelRecommendationTier.PREMIUM, LlmModel.recommendationTier(LlmModel.GEMINI_3_PRO))
        assertEquals(ModelRecommendationTier.DEFAULT, LlmModel.recommendationTier(LlmModel.GEMINI_3_5_FLASH))
        assertEquals(ModelRecommendationTier.CHEAPER, LlmModel.recommendationTier(LlmModel.GEMINI_3_FLASH))
    }

    @Test
    fun preferredDefaultModelChoosesClaudeSonnetOverOpusWhenAvailable() {
        val preferred = LlmModel.preferredDefaultModel(
            LlmProvider.ANTHROPIC,
            listOf(LlmModel.CLAUDE_OPUS_4_6, LlmModel.CLAUDE_SONNET_4_5, LlmModel.CLAUDE_HAIKU_4_5)
        )

        assertEquals(LlmModel.CLAUDE_SONNET_4_5, preferred)
    }
}
