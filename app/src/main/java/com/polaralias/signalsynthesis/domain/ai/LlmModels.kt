package com.polaralias.signalsynthesis.domain.ai

enum class LlmProvider {
    OPENAI,
    GEMINI
}

enum class LlmModel(val provider: LlmProvider, val modelId: String) {
    // OpenAI Models (current generation)
    GPT_5_2(LlmProvider.OPENAI, "gpt-5.2"),
    GPT_5_1(LlmProvider.OPENAI, "gpt-5.1"),
    GPT_5_MINI(LlmProvider.OPENAI, "gpt-5-mini"),
    GPT_5_NANO(LlmProvider.OPENAI, "gpt-5-nano"),

    // Gemini Models (latest generation)
    GEMINI_3_FLASH(LlmProvider.GEMINI, "gemini-3-flash"),
    GEMINI_3_PRO(LlmProvider.GEMINI, "gemini-3-pro")
}

/**
 * Maps to 'reasoning_effort' for OpenAI and 'thinking_level' for Gemini.
 */
enum class ReasoningDepth { 
    NONE,
    MINIMAL, 
    LOW, 
    MEDIUM, 
    HIGH, 
    EXTRA 
}

enum class OutputLength { SHORT, STANDARD, FULL }
enum class Verbosity { LOW, MEDIUM, HIGH }
