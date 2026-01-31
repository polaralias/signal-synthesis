package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.AiSummaryRepository
import com.polaralias.signalsynthesis.domain.model.AiSummaryCacheKey
import com.polaralias.signalsynthesis.domain.model.AiSynthesis
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.polaralias.signalsynthesis.data.provider.RetryHelper
import com.polaralias.signalsynthesis.util.Logger

class PrefetchAiSummariesUseCase(
    private val synthesizeUseCase: SynthesizeSetupUseCase,
    private val aiSummaryRepository: AiSummaryRepository
) {
    suspend fun execute(
        setups: List<TradeSetup>,
        llmKey: String,
        maxPrefetch: Int = 3,
        cacheKeyProvider: (TradeSetup) -> AiSummaryCacheKey
    ): Flow<Pair<String, AiSynthesis>> = flow {
        val targets = setups.take(maxPrefetch)
        for (setup in targets) {
            val cacheKey = cacheKeyProvider(setup)
            // Check cache first
            val cached = aiSummaryRepository.getSummary(setup.symbol, cacheKey.model, cacheKey.promptHash)
            if (cached != null) {
                emit(setup.symbol to cached)
                continue
            }

            // Generate and cache
            try {
                val synthesis = RetryHelper.withRetry("Prefetch") {
                    synthesizeUseCase.execute(setup = setup, llmKey = llmKey)
                }
                aiSummaryRepository.saveSummary(setup.symbol, cacheKey.model, cacheKey.promptHash, synthesis)
                emit(setup.symbol to synthesis)
            } catch (e: Exception) {
                Logger.e("Prefetch", "Failed to prefetch for ${setup.symbol}", e)
                // Skip if failed
            }
        }
    }
}
