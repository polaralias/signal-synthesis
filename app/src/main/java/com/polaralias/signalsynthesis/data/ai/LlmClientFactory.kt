package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient
import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.LlmProvider

open class LlmClientFactory {
    private val openAiService by lazy { OpenAiService.create() }
    private val geminiService by lazy { GeminiService.create() }

    open fun create(model: LlmModel): LlmClient {
        return when (model.provider) {
            LlmProvider.OPENAI -> OpenAiLlmClient(openAiService, model.modelId)
            LlmProvider.GEMINI -> GeminiLlmClient(geminiService, model.modelId)
        }
    }
}
