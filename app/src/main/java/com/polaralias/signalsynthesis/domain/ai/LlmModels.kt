package com.polaralias.signalsynthesis.domain.ai

enum class LlmApiFormat {
    ANTHROPIC,
    OPENAI_CHAT,
    OPENAI_RESPONSES,
    GOOGLE_GEMINI,
    OPENAI_COMPATIBLE
}

enum class LlmModelVisibilityGroup {
    CORE_REASONING,
    EXECUTION_AUTOMATION,
    ADDITIONAL
}

enum class LlmProvider(
    val providerId: String,
    val displayName: String,
    val baseUrl: String,
    val apiFormat: LlmApiFormat,
    val requiresApiKey: Boolean = true
) {
    ANTHROPIC(
        providerId = "anthropic",
        displayName = "Anthropic",
        baseUrl = "https://api.anthropic.com",
        apiFormat = LlmApiFormat.ANTHROPIC
    ),
    OPENAI(
        providerId = "openai",
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com",
        apiFormat = LlmApiFormat.OPENAI_RESPONSES
    ),
    GEMINI(
        providerId = "google",
        displayName = "Google Gemini",
        baseUrl = "https://generativelanguage.googleapis.com",
        apiFormat = LlmApiFormat.GOOGLE_GEMINI
    ),
    MINIMAX(
        providerId = "minimax",
        displayName = "MiniMax",
        baseUrl = "https://api.minimaxi.com/v1",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE
    ),
    OPENROUTER(
        providerId = "openrouter",
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE
    ),
    TOGETHER(
        providerId = "together",
        displayName = "Together AI",
        baseUrl = "https://api.together.xyz/v1",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE
    ),
    GROQ(
        providerId = "groq",
        displayName = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE
    ),
    DEEPSEEK(
        providerId = "deepseek",
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE
    ),
    SILICONFLOW(
        providerId = "siliconflow",
        displayName = "SiliconFlow",
        baseUrl = "https://api.siliconflow.com/v1",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE
    ),
    OLLAMA(
        providerId = "ollama",
        displayName = "Ollama",
        baseUrl = "http://localhost:11434",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE,
        requiresApiKey = false
    ),
    LOCALAI(
        providerId = "localai",
        displayName = "LocalAI",
        baseUrl = "http://localhost:8080",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE,
        requiresApiKey = false
    ),
    VLLM(
        providerId = "vllm",
        displayName = "vLLM",
        baseUrl = "http://localhost:8000",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE,
        requiresApiKey = false
    ),
    TGI(
        providerId = "tgi",
        displayName = "TGI",
        baseUrl = "http://localhost:8080",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE,
        requiresApiKey = false
    ),
    SGLANG(
        providerId = "sglang",
        displayName = "SGLang",
        baseUrl = "http://localhost:30000",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE,
        requiresApiKey = false
    ),
    CUSTOM(
        providerId = "custom",
        displayName = "Custom",
        baseUrl = "http://localhost:8000",
        apiFormat = LlmApiFormat.OPENAI_COMPATIBLE
    );

    fun supportsWebTools(): Boolean {
        return this == OPENAI || this == GEMINI
    }

    companion object {
        fun fromProviderId(providerId: String): LlmProvider {
            return values().firstOrNull { it.providerId.equals(providerId, ignoreCase = true) } ?: OPENAI
        }
    }
}

enum class LlmModel(
    val provider: LlmProvider,
    val modelId: String,
    val label: String,
    val description: String,
    val visibilityGroup: LlmModelVisibilityGroup = LlmModelVisibilityGroup.ADDITIONAL,
    val lowCost: Boolean = false,
    val apiFormatOverride: LlmApiFormat? = null
) {
    // Anthropic
    CLAUDE_OPUS_4_6(
        provider = LlmProvider.ANTHROPIC,
        modelId = "claude-opus-4-6",
        label = "Claude Opus 4.6",
        description = "Deep reasoning for complex synthesis",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING
    ),
    CLAUDE_SONNET_4_5(
        provider = LlmProvider.ANTHROPIC,
        modelId = "claude-sonnet-4-5",
        label = "Claude Sonnet 4.5",
        description = "Balanced quality and speed",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING
    ),
    CLAUDE_HAIKU_4_5(
        provider = LlmProvider.ANTHROPIC,
        modelId = "claude-haiku-4-5",
        label = "Claude Haiku 4.5",
        description = "Low-latency and cost efficient",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING,
        lowCost = true
    ),

    // OpenAI (keep legacy enum names for settings compatibility)
    GPT_5_2(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5.2",
        label = "GPT-5.2",
        description = "OpenAI reasoning flagship",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    GPT_5_MINI(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5-mini",
        label = "GPT-5 Mini",
        description = "Quick and cost-efficient",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING,
        lowCost = true,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    GPT_5_NANO(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5-nano",
        label = "GPT-5 Nano",
        description = "Lowest-cost GPT-5 tier",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING,
        lowCost = true,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    GPT_5_1(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5.1",
        label = "GPT-5.1",
        description = "General use at lower cost",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING,
        lowCost = true,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    GPT_5_3_CODEX(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5.3-codex",
        label = "GPT-5.3 Codex",
        description = "Code and execution-focused frontier model",
        visibilityGroup = LlmModelVisibilityGroup.EXECUTION_AUTOMATION,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    GPT_5_2_CODEX(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5.2-codex",
        label = "GPT-5.2 Codex",
        description = "Stable code and execution workflows",
        visibilityGroup = LlmModelVisibilityGroup.EXECUTION_AUTOMATION,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    GPT_5_1_CODEX(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5.1-codex",
        label = "GPT-5.1 Codex",
        description = "General coding and execution tasks",
        visibilityGroup = LlmModelVisibilityGroup.EXECUTION_AUTOMATION,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    GPT_5_1_CODEX_MINI(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5.1-codex-mini",
        label = "GPT-5.1 Codex Mini",
        description = "Lower-cost execution model",
        visibilityGroup = LlmModelVisibilityGroup.EXECUTION_AUTOMATION,
        lowCost = true,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    GPT_5_1_CODEX_MAX(
        provider = LlmProvider.OPENAI,
        modelId = "gpt-5.1-codex-max",
        label = "GPT-5.1 Codex Max",
        description = "Highest-capability codex variant",
        visibilityGroup = LlmModelVisibilityGroup.EXECUTION_AUTOMATION,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),
    COMPUTER_USE_PREVIEW(
        provider = LlmProvider.OPENAI,
        modelId = "computer-use-preview",
        label = "Computer Use Preview",
        description = "OpenAI computer-use preview",
        visibilityGroup = LlmModelVisibilityGroup.EXECUTION_AUTOMATION,
        apiFormatOverride = LlmApiFormat.OPENAI_RESPONSES
    ),

    // Google Gemini (keep legacy enum names for compatibility)
    GEMINI_3_PRO(
        provider = LlmProvider.GEMINI,
        modelId = "gemini-3-pro-preview-09-2026",
        label = "Gemini 3 Pro",
        description = "Reasoning-heavy long-context workflows",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING
    ),
    GEMINI_3_FLASH(
        provider = LlmProvider.GEMINI,
        modelId = "gemini-3-flash-preview-09-2026",
        label = "Gemini 3 Flash",
        description = "Fast, low-cost Gemini 3",
        visibilityGroup = LlmModelVisibilityGroup.CORE_REASONING,
        lowCost = true
    ),
    GEMINI_2_5_PRO(
        provider = LlmProvider.GEMINI,
        modelId = "gemini-2.5-pro",
        label = "Gemini 2.5 Pro",
        description = "Previous Gemini flagship"
    ),
    GEMINI_2_5_FLASH(
        provider = LlmProvider.GEMINI,
        modelId = "gemini-2.5-flash",
        label = "Gemini 2.5 Flash",
        description = "Fast Gemini 2.5",
        lowCost = true
    ),

    // MiniMax
    MINIMAX_M2(
        provider = LlmProvider.MINIMAX,
        modelId = "M2",
        label = "MiniMax M2",
        description = "MiniMax flagship"
    ),
    MINIMAX_M2_PRO(
        provider = LlmProvider.MINIMAX,
        modelId = "M2-Pro",
        label = "MiniMax M2 Pro",
        description = "Higher-capability M2"
    ),

    // Ollama
    OLLAMA_LLAMA3_3_LATEST(
        provider = LlmProvider.OLLAMA,
        modelId = "llama3.3:latest",
        label = "Llama 3.3 8B",
        description = "Local open-source model",
        lowCost = true
    ),
    OLLAMA_LLAMA3_3_70B(
        provider = LlmProvider.OLLAMA,
        modelId = "llama3.3:70b",
        label = "Llama 3.3 70B",
        description = "Large local model"
    ),
    OLLAMA_QWEN2_5_LATEST(
        provider = LlmProvider.OLLAMA,
        modelId = "qwen2.5:latest",
        label = "Qwen 2.5 7B",
        description = "Local Qwen baseline",
        lowCost = true
    ),
    OLLAMA_QWEN2_5_32B(
        provider = LlmProvider.OLLAMA,
        modelId = "qwen2.5:32b",
        label = "Qwen 2.5 32B",
        description = "Large local Qwen"
    ),
    OLLAMA_DEEPSEEK_R1(
        provider = LlmProvider.OLLAMA,
        modelId = "deepseek-r1:latest",
        label = "DeepSeek R1",
        description = "Local reasoning model"
    ),
    OLLAMA_CODELLAMA(
        provider = LlmProvider.OLLAMA,
        modelId = "codellama:latest",
        label = "Code Llama",
        description = "Local coding model"
    ),
    OLLAMA_MISTRAL(
        provider = LlmProvider.OLLAMA,
        modelId = "mistral:latest",
        label = "Mistral 7B",
        description = "Local Mistral",
        lowCost = true
    ),
    OLLAMA_PHI3(
        provider = LlmProvider.OLLAMA,
        modelId = "phi3:latest",
        label = "Phi-3",
        description = "Small local model",
        lowCost = true
    ),
    LOCALAI_COMPATIBLE(
        provider = LlmProvider.LOCALAI,
        modelId = "localai-default-model",
        label = "LocalAI Compatible Model",
        description = "Replace with any LocalAI-hosted model ID",
        lowCost = true
    ),
    VLLM_COMPATIBLE(
        provider = LlmProvider.VLLM,
        modelId = "vllm-default-model",
        label = "vLLM Compatible Model",
        description = "Replace with any vLLM-hosted model ID",
        lowCost = true
    ),
    TGI_COMPATIBLE(
        provider = LlmProvider.TGI,
        modelId = "tgi-default-model",
        label = "TGI Compatible Model",
        description = "Replace with any TGI-hosted model ID",
        lowCost = true
    ),
    SGLANG_COMPATIBLE(
        provider = LlmProvider.SGLANG,
        modelId = "sglang-default-model",
        label = "SGLang Compatible Model",
        description = "Replace with any SGLang-hosted model ID",
        lowCost = true
    ),

    // OpenRouter
    OPENROUTER_CLAUDE_SONNET_4_5(
        provider = LlmProvider.OPENROUTER,
        modelId = "anthropic/claude-sonnet-4-5",
        label = "Claude Sonnet 4.5 (OpenRouter)",
        description = "Anthropic via OpenRouter"
    ),
    OPENROUTER_GPT_5_2(
        provider = LlmProvider.OPENROUTER,
        modelId = "openai/gpt-5.2",
        label = "GPT-5.2 (OpenRouter)",
        description = "OpenAI via OpenRouter"
    ),
    OPENROUTER_LLAMA3_3_70B(
        provider = LlmProvider.OPENROUTER,
        modelId = "meta-llama/llama-3.3-70b-instruct",
        label = "Llama 3.3 70B (OpenRouter)",
        description = "Meta via OpenRouter"
    ),
    OPENROUTER_DEEPSEEK_R1(
        provider = LlmProvider.OPENROUTER,
        modelId = "deepseek/deepseek-r1",
        label = "DeepSeek R1 (OpenRouter)",
        description = "DeepSeek via OpenRouter"
    ),

    // Together
    TOGETHER_LLAMA3_3_70B_TURBO(
        provider = LlmProvider.TOGETHER,
        modelId = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
        label = "Llama 3.3 70B Turbo (Together)",
        description = "Meta via Together"
    ),
    TOGETHER_QWEN2_5_72B_TURBO(
        provider = LlmProvider.TOGETHER,
        modelId = "Qwen/Qwen2.5-72B-Instruct-Turbo",
        label = "Qwen 2.5 72B Turbo (Together)",
        description = "Qwen via Together"
    ),

    // Groq
    GROQ_GPT_OSS_120B(
        provider = LlmProvider.GROQ,
        modelId = "openai/gpt-oss-120b",
        label = "GPT-OSS 120B (Groq)",
        description = "Open model via Groq"
    ),
    GROQ_LLAMA3_3_70B(
        provider = LlmProvider.GROQ,
        modelId = "llama-3.3-70b-versatile",
        label = "Llama 3.3 70B (Groq)",
        description = "Llama on Groq"
    ),
    GROQ_MIXTRAL_8X7B(
        provider = LlmProvider.GROQ,
        modelId = "mixtral-8x7b-32768",
        label = "Mixtral 8x7B (Groq)",
        description = "Mixtral on Groq",
        lowCost = true
    ),

    // DeepSeek
    DEEPSEEK_CHAT(
        provider = LlmProvider.DEEPSEEK,
        modelId = "deepseek-chat",
        label = "DeepSeek Chat",
        description = "DeepSeek official chat",
        lowCost = true
    ),
    DEEPSEEK_REASONER(
        provider = LlmProvider.DEEPSEEK,
        modelId = "deepseek-reasoner",
        label = "DeepSeek Reasoner",
        description = "Reasoning-tuned DeepSeek"
    ),

    // SiliconFlow
    SILICONFLOW_QWEN2_5_72B(
        provider = LlmProvider.SILICONFLOW,
        modelId = "Qwen/Qwen2.5-72B-Instruct",
        label = "Qwen 2.5 72B (SiliconFlow)",
        description = "Qwen via SiliconFlow"
    ),
    SILICONFLOW_DEEPSEEK_V3(
        provider = LlmProvider.SILICONFLOW,
        modelId = "deepseek-ai/DeepSeek-V3",
        label = "DeepSeek V3 (SiliconFlow)",
        description = "DeepSeek via SiliconFlow"
    ),

    // Custom
    CUSTOM_MODEL(
        provider = LlmProvider.CUSTOM,
        modelId = "custom-model",
        label = "Custom Model",
        description = "Manual model ID / endpoint"
    );

    fun resolvedApiFormat(): LlmApiFormat {
        return apiFormatOverride ?: provider.apiFormat
    }

    companion object {
        private val modelById = values().associateBy { it.modelId.lowercase() }

        private val modelIdAliases = mapOf(
            "gpt-5" to "gpt-5.2",
            "gpt-5.2-mini" to "gpt-5-mini",
            "gpt-5.1-mini" to "gpt-5-mini",
            "gpt-5.2-nano" to "gpt-5-nano",
            "gpt-5.1-nano" to "gpt-5-nano",
            "gpt-4o" to "gpt-5-mini",
            "gemini-3-flash" to "gemini-3-flash-preview-09-2026",
            "gemini-3-pro" to "gemini-3-pro-preview-09-2026"
        )

        fun normalizeModelIdAlias(modelId: String): String {
            val trimmed = modelId.trim()
            if (trimmed.isEmpty()) return ""
            val canonical = trimmed.lowercase().replace(Regex("[_\\s]+"), "-")
            val alias = modelIdAliases[canonical]
            if (alias != null) return alias
            val exact = modelById[canonical]
            return exact?.modelId ?: trimmed
        }

        fun fromModelId(modelId: String): LlmModel? {
            val normalized = normalizeModelIdAlias(modelId)
            return modelById[normalized.lowercase()]
        }

        fun modelsForProvider(provider: LlmProvider): List<LlmModel> {
            return values().filter { it.provider == provider }
        }

        fun inferProvider(modelId: String): LlmProvider {
            val normalized = normalizeModelIdAlias(modelId)
            val known = fromModelId(normalized)
            if (known != null) return known.provider

            val lower = normalized.lowercase()
            if (
                lower.startsWith("anthropic/") ||
                lower.startsWith("openai/") ||
                lower.startsWith("meta-llama/") ||
                lower.startsWith("deepseek/")
            ) {
                return LlmProvider.OPENROUTER
            }
            if (lower.contains(":")) {
                return LlmProvider.OLLAMA
            }
            if (lower.contains("claude")) return LlmProvider.ANTHROPIC
            if (lower.contains("gemini")) return LlmProvider.GEMINI
            if (lower.contains("minimax") || lower.startsWith("m2")) return LlmProvider.MINIMAX
            if (
                lower.startsWith("gpt-") ||
                lower.startsWith("o1") ||
                lower.startsWith("o3") ||
                lower.startsWith("o4") ||
                lower.startsWith("computer-use") ||
                lower.startsWith("chatgpt")
            ) {
                return LlmProvider.OPENAI
            }
            return LlmProvider.ANTHROPIC
        }

        fun usesOpenAiResponsesApi(modelId: String): Boolean {
            val normalized = normalizeModelIdAlias(modelId)
            val knownModel = fromModelId(normalized)
            if (knownModel != null) {
                return knownModel.provider == LlmProvider.OPENAI &&
                    knownModel.resolvedApiFormat() == LlmApiFormat.OPENAI_RESPONSES
            }
            return inferProvider(normalized) == LlmProvider.OPENAI
        }

        fun isReasoningFamily(modelId: String): Boolean {
            val normalized = normalizeModelIdAlias(modelId).lowercase()
            return normalized.startsWith("o1") ||
                normalized.startsWith("o3") ||
                normalized.startsWith("o4") ||
                normalized.startsWith("gpt-5") ||
                normalized.contains("gpt-5") ||
                normalized.startsWith("computer-use") ||
                normalized.contains("-o1") ||
                normalized.contains("-o3") ||
                normalized.contains("-o4") ||
                normalized.contains("o1-") ||
                normalized.contains("o3-") ||
                normalized.contains("o4-")
        }

        fun isLegacyOpenAiModel(modelId: String): Boolean {
            val normalized = normalizeModelIdAlias(modelId).lowercase()
            return normalized.contains("gpt-3.5") ||
                (normalized.contains("gpt-4") &&
                    !normalized.contains("gpt-4o") &&
                    !normalized.contains("gpt-4-turbo"))
        }

        fun supportsCustomTemperature(modelId: String): Boolean {
            return !isReasoningFamily(modelId)
        }

        fun supportsNativeReasoningControl(provider: LlmProvider, modelId: String): Boolean {
            val normalized = normalizeModelIdAlias(modelId).lowercase()
            return when (provider) {
                LlmProvider.OPENAI -> isReasoningFamily(normalized)
                LlmProvider.GEMINI -> normalized.startsWith("gemini-3")
                else -> false
            }
        }
    }
}

/**
 * Maps to 'reasoning_effort' for OpenAI and 'thinking_level' for Gemini when supported.
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
