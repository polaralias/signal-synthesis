package com.polaralias.signalsynthesis.domain.ai

interface LlmClient {
    suspend fun generate(prompt: String, systemPrompt: String? = null, apiKey: String): String
}
