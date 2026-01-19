package com.polaralias.signalsynthesis.domain.ai

enum class LlmProvider {
    OPENAI,
    GEMINI
}

enum class LlmModel(val provider: LlmProvider, val modelId: String) {
    // OpenAI Models (as per Phase 3 Spec)
    GPT_5_2(LlmProvider.OPENAI, "gpt-5.2"),
    GPT_5_1(LlmProvider.OPENAI, "gpt-5.1"),
    GPT_5_MINI(LlmProvider.OPENAI, "gpt-5-mini"),
    GPT_5_NANO(LlmProvider.OPENAI, "gpt-5-nano"),
    GPT_5_2_PRO(LlmProvider.OPENAI, "gpt-5.2-pro"),

    // Gemini Models (as per Phase 3 Spec)
    GEMINI_2_5_FLASH(LlmProvider.GEMINI, "gemini-2.5-flash"),
    GEMINI_2_5_PRO(LlmProvider.GEMINI, "gemini-2.5-pro"),
    GEMINI_3_FLASH(LlmProvider.GEMINI, "gemini-3-flash"),
    GEMINI_3_PRO(LlmProvider.GEMINI, "gemini-3-pro")
}

enum class ReasoningDepth { FAST, BALANCED, DEEP, EXTRA }
enum class OutputLength { SHORT, STANDARD, FULL }
enum class Verbosity { LOW, MEDIUM, HIGH }
