package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmProvider

interface ProviderModelDiscovery {
    suspend fun discoverModelIds(provider: LlmProvider, apiKey: String): List<String>
}

class LiveProviderModelDiscovery(
    private val openAiResponsesService: OpenAiResponsesService = OpenAiResponsesService.create(),
    private val geminiService: GeminiService = GeminiService.create(),
    private val anthropicService: AnthropicService = AnthropicService.create()
) : ProviderModelDiscovery {
    override suspend fun discoverModelIds(provider: LlmProvider, apiKey: String): List<String> {
        if (apiKey.isBlank()) return emptyList()

        return when (provider) {
            LlmProvider.OPENAI -> openAiResponsesService
                .listModels("Bearer $apiKey")
                .data
                .map { it.id }
            LlmProvider.GEMINI -> geminiService
                .listModels(apiVersion = "v1beta", apiKey = apiKey)
                .models
                .filter { model ->
                    model.supportedGenerationMethods.any { method ->
                        method.equals("generateContent", ignoreCase = true)
                    }
                }
                .map { it.name }
            LlmProvider.ANTHROPIC -> anthropicService
                .listModels(apiKey = apiKey)
                .data
                .map { it.id }
            else -> emptyList()
        }
    }
}
