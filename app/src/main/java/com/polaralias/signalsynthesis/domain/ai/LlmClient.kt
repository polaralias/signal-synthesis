package com.polaralias.signalsynthesis.domain.ai

interface LlmClient {
    suspend fun generate(
        prompt: String,
        systemPrompt: String? = null,
        apiKey: String,
        reasoningDepth: ReasoningDepth = ReasoningDepth.BALANCED,
        outputLength: OutputLength = OutputLength.STANDARD,
        verbosity: Verbosity = Verbosity.MEDIUM
    ): String
}
