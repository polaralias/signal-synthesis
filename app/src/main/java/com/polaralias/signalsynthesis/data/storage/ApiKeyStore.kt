package com.polaralias.signalsynthesis.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.polaralias.signalsynthesis.data.provider.ApiKeys
import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ApiKeyStorage {
    suspend fun loadApiKeys(): ApiKeys
    suspend fun loadLlmKeys(): LlmKeys
    suspend fun saveKeys(apiKeys: ApiKeys, llmKeys: LlmKeys)
    suspend fun clear()
}

data class LlmKeys(
    val anthropicKey: String? = null,
    val openAiKey: String? = null,
    val geminiKey: String? = null,
    val minimaxKey: String? = null,
    val openRouterKey: String? = null,
    val togetherKey: String? = null,
    val groqKey: String? = null,
    val deepseekKey: String? = null,
    val siliconFlowKey: String? = null,
    val customKey: String? = null
) {
    fun hasAny(): Boolean {
        return listOf(
            anthropicKey,
            openAiKey,
            geminiKey,
            minimaxKey,
            openRouterKey,
            togetherKey,
            groqKey,
            deepseekKey,
            siliconFlowKey,
            customKey
        ).any { !it.isNullOrBlank() }
    }

    fun toProviderMap(): Map<LlmProvider, String> {
        return mapOf(
            LlmProvider.ANTHROPIC to anthropicKey.orEmpty(),
            LlmProvider.OPENAI to openAiKey.orEmpty(),
            LlmProvider.GEMINI to geminiKey.orEmpty(),
            LlmProvider.MINIMAX to minimaxKey.orEmpty(),
            LlmProvider.OPENROUTER to openRouterKey.orEmpty(),
            LlmProvider.TOGETHER to togetherKey.orEmpty(),
            LlmProvider.GROQ to groqKey.orEmpty(),
            LlmProvider.DEEPSEEK to deepseekKey.orEmpty(),
            LlmProvider.SILICONFLOW to siliconFlowKey.orEmpty(),
            LlmProvider.OLLAMA to "",
            LlmProvider.LOCALAI to "",
            LlmProvider.VLLM to "",
            LlmProvider.TGI to "",
            LlmProvider.SGLANG to "",
            LlmProvider.CUSTOM to customKey.orEmpty()
        )
    }
}

class ApiKeyStore(context: Context) : ApiKeyStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun loadApiKeys(): ApiKeys = withContext(Dispatchers.IO) {
        ApiKeys(
            alpacaKey = preferences.getString(KEY_ALPACA, null),
            alpacaSecret = preferences.getString(KEY_ALPACA_SECRET, null),
            massive = preferences.getString(KEY_POLYGON, null),
            finnhub = preferences.getString(KEY_FINNHUB, null),
            financialModelingPrep = preferences.getString(KEY_FMP, null),
            twelveData = preferences.getString(KEY_TWELVE_DATA, null)
        )
    }

    override suspend fun loadLlmKeys(): LlmKeys = withContext(Dispatchers.IO) {
        LlmKeys(
            anthropicKey = preferences.getString(KEY_ANTHROPIC, null),
            openAiKey = preferences.getString(KEY_OPENAI, null),
            geminiKey = preferences.getString(KEY_GEMINI, null),
            minimaxKey = preferences.getString(KEY_MINIMAX, null),
            openRouterKey = preferences.getString(KEY_OPENROUTER, null),
            togetherKey = preferences.getString(KEY_TOGETHER, null),
            groqKey = preferences.getString(KEY_GROQ, null),
            deepseekKey = preferences.getString(KEY_DEEPSEEK, null),
            siliconFlowKey = preferences.getString(KEY_SILICONFLOW, null),
            customKey = preferences.getString(KEY_CUSTOM, null)
        )
    }

    override suspend fun saveKeys(apiKeys: ApiKeys, llmKeys: LlmKeys) {
        withContext(Dispatchers.IO) {
            preferences.edit().apply {
                putOrRemove(KEY_ALPACA, apiKeys.alpacaKey)
                putOrRemove(KEY_ALPACA_SECRET, apiKeys.alpacaSecret)
                putOrRemove(KEY_POLYGON, apiKeys.massive)
                putOrRemove(KEY_FINNHUB, apiKeys.finnhub)
                putOrRemove(KEY_FMP, apiKeys.financialModelingPrep)
                putOrRemove(KEY_TWELVE_DATA, apiKeys.twelveData)

                putOrRemove(KEY_ANTHROPIC, llmKeys.anthropicKey)
                putOrRemove(KEY_OPENAI, llmKeys.openAiKey)
                putOrRemove(KEY_GEMINI, llmKeys.geminiKey)
                putOrRemove(KEY_MINIMAX, llmKeys.minimaxKey)
                putOrRemove(KEY_OPENROUTER, llmKeys.openRouterKey)
                putOrRemove(KEY_TOGETHER, llmKeys.togetherKey)
                putOrRemove(KEY_GROQ, llmKeys.groqKey)
                putOrRemove(KEY_DEEPSEEK, llmKeys.deepseekKey)
                putOrRemove(KEY_SILICONFLOW, llmKeys.siliconFlowKey)
                putOrRemove(KEY_CUSTOM, llmKeys.customKey)
                apply()
            }
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit().clear().apply()
    }

    private fun android.content.SharedPreferences.Editor.putOrRemove(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            remove(key)
        } else {
            putString(key, value.trim())
        }
    }

    companion object {
        private const val PREFS_NAME = "signal_synthesis_keys"

        private const val KEY_ALPACA = "alpaca_key"
        private const val KEY_ALPACA_SECRET = "alpaca_secret"
        private const val KEY_POLYGON = "polygon_key"
        private const val KEY_FINNHUB = "finnhub_key"
        private const val KEY_FMP = "fmp_key"
        private const val KEY_TWELVE_DATA = "twelve_data_key"

        private const val KEY_ANTHROPIC = "anthropic_key"
        private const val KEY_OPENAI = "openai_key"
        private const val KEY_GEMINI = "gemini_key"
        private const val KEY_MINIMAX = "minimax_key"
        private const val KEY_OPENROUTER = "openrouter_key"
        private const val KEY_TOGETHER = "together_key"
        private const val KEY_GROQ = "groq_key"
        private const val KEY_DEEPSEEK = "deepseek_key"
        private const val KEY_SILICONFLOW = "siliconflow_key"
        private const val KEY_CUSTOM = "custom_llm_key"
    }
}
