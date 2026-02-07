package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.LlmProvider

open class LlmClientFactory {
    private val openAiResponsesService by lazy { OpenAiResponsesService.create() }
    private val anthropicService by lazy { AnthropicService.create() }
    private val geminiService by lazy { GeminiService.create() }
    private val openAiCompatibleService by lazy { OpenAiCompatibleService.create() }

    open fun create(model: LlmModel): LlmClient {
        return when (model.provider) {
            LlmProvider.OPENAI -> OpenAiLlmClient(openAiResponsesService, model.modelId)
            LlmProvider.GEMINI -> GeminiLlmClient(geminiService, model.modelId)
            LlmProvider.ANTHROPIC -> AnthropicLlmClient(anthropicService, model.modelId)
            else -> OpenAiCompatibleLlmClient(openAiCompatibleService, model.provider, model.modelId)
        }
    }
}
