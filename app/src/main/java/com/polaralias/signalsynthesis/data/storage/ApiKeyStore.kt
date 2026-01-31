package com.polaralias.signalsynthesis.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.polaralias.signalsynthesis.data.provider.ApiKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ApiKeyStorage {
    suspend fun loadApiKeys(): ApiKeys
    suspend fun loadLlmKeys(): LlmKeys
    suspend fun saveKeys(apiKeys: ApiKeys, llmKeys: LlmKeys)
    suspend fun clear()
}

data class LlmKeys(
    val openAiKey: String? = null,
    val geminiKey: String? = null
)

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
            openAiKey = preferences.getString(KEY_OPENAI, null),
            geminiKey = preferences.getString(KEY_GEMINI, null)
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
                putOrRemove(KEY_OPENAI, llmKeys.openAiKey)
                putOrRemove(KEY_GEMINI, llmKeys.geminiKey)
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
        private const val KEY_OPENAI = "openai_key"
        private const val KEY_GEMINI = "gemini_key"
    }
}
