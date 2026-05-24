package com.polaralias.signalsynthesis.data.ai

import com.polaralias.signalsynthesis.data.settings.AppSettings
import com.polaralias.signalsynthesis.data.storage.LlmKeys
import com.polaralias.signalsynthesis.domain.ai.LlmModel
import com.polaralias.signalsynthesis.domain.ai.LlmProvider

class ProviderModelCatalogService(
    private val providerModelDiscovery: ProviderModelDiscovery
) {
    suspend fun discoverAvailableModels(
        llmKeys: LlmKeys,
        onDiscoveryFailure: (LlmProvider, Throwable) -> Unit = { _, _ -> }
    ): Map<LlmProvider, List<LlmModel>> {
        val discovered = mutableMapOf<LlmProvider, List<LlmModel>>()
        val keyMap = llmKeys.toProviderMap()
        listOf(
            LlmProvider.OPENAI,
            LlmProvider.GEMINI,
            LlmProvider.ANTHROPIC
        ).forEach { provider ->
            val apiKey = keyMap[provider].orEmpty()
            if (apiKey.isBlank()) return@forEach

            val availableIds = runCatching {
                providerModelDiscovery.discoverModelIds(provider, apiKey)
            }.onFailure { error ->
                onDiscoveryFailure(provider, error)
            }.getOrDefault(emptyList())

            val availableModels = LlmModel.availableModelsForProvider(provider, availableIds.toSet())
            if (availableModels.isNotEmpty()) {
                discovered[provider] = availableModels
            }
        }
        return discovered
    }

    fun alignSettingsWithDiscoveredModels(
        settings: AppSettings,
        discoveredModels: Map<LlmProvider, List<LlmModel>>
    ): AppSettings {
        var updated = settings
        discoveredModels.forEach { (provider, availableModels) ->
            if (availableModels.isEmpty()) return@forEach
            val preferred = LlmModel.preferredDefaultModel(provider, availableModels) ?: availableModels.first()
            val availableSet = availableModels.toSet()

            if (updated.llmProvider == provider) {
                updated = updated.copy(
                    analysisModel = alignProviderModel(updated.analysisModel, provider, availableSet, preferred),
                    verdictModel = alignProviderModel(updated.verdictModel, provider, availableSet, preferred)
                )
            }

            if (updated.deepDiveProvider == provider) {
                updated = updated.copy(
                    reasoningModel = alignProviderModel(updated.reasoningModel, provider, availableSet, preferred)
                )
            }
        }
        return updated
    }

    private fun alignProviderModel(
        current: LlmModel,
        provider: LlmProvider,
        availableModels: Set<LlmModel>,
        preferredModel: LlmModel
    ): LlmModel {
        if (current.provider != provider) {
            return preferredModel
        }
        if (!availableModels.contains(current)) {
            return preferredModel
        }
        if (isLegacyDefaultModel(current)) {
            return preferredModel
        }
        return current
    }

    private fun isLegacyDefaultModel(model: LlmModel): Boolean {
        return when (model) {
            LlmModel.GPT_5_1,
            LlmModel.GPT_5_2,
            LlmModel.GEMINI_2_5_PRO,
            LlmModel.GEMINI_2_5_FLASH -> true
            else -> false
        }
    }
}
