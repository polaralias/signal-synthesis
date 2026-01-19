package com.polaralias.signalsynthesis.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.polaralias.signalsynthesis.data.provider.ApiKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ApiKeyStorage {
    suspend fun loadApiKeys(): ApiKeys
    suspend fun loadLlmKey(): String?
    suspend fun saveKeys(apiKeys: ApiKeys, llmKey: String?)
    suspend fun clear()
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
            polygon = preferences.getString(KEY_POLYGON, null),
            finnhub = preferences.getString(KEY_FINNHUB, null),
            financialModelingPrep = preferences.getString(KEY_FMP, null),
            twelveData = preferences.getString(KEY_TWELVE_DATA, null)
        )
    }

    override suspend fun loadLlmKey(): String? = withContext(Dispatchers.IO) {
        preferences.getString(KEY_LLM, null)
    }

    override suspend fun saveKeys(apiKeys: ApiKeys, llmKey: String?) {
        withContext(Dispatchers.IO) {
            preferences.edit().apply {
                putOrRemove(KEY_ALPACA, apiKeys.alpacaKey)
                putOrRemove(KEY_ALPACA_SECRET, apiKeys.alpacaSecret)
                putOrRemove(KEY_POLYGON, apiKeys.polygon)
                putOrRemove(KEY_FINNHUB, apiKeys.finnhub)
                putOrRemove(KEY_FMP, apiKeys.financialModelingPrep)
                putOrRemove(KEY_TWELVE_DATA, apiKeys.twelveData)
                putOrRemove(KEY_LLM, llmKey)
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
        private const val KEY_LLM = "llm_key"
    }
}
