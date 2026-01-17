package com.polaralias.signalsynthesis.domain.ai

interface LlmClient {
    suspend fun generate(prompt: String, apiKey: String): String
}
