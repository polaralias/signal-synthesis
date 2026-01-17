package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.domain.ai.LlmClient

class OpenAiLlmClient(
    private val service: OpenAiService,
    private val model: String = DEFAULT_MODEL
) : LlmClient {
    override suspend fun generate(prompt: String, apiKey: String): String {
        val request = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(
                    role = ROLE_SYSTEM,
                    content = "You are a senior trading analyst. Respond with JSON only."
                ),
                OpenAiMessage(
                    role = ROLE_USER,
                    content = prompt
                )
            )
        )
        val response = service.createChatCompletion("Bearer $apiKey", request)
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }

    companion object {
        private const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"
    }
}
