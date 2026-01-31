package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import java.time.Clock

/**
 * Main pipeline orchestration use case that runs the complete analysis flow:
 * 1. Discover candidates
 * 2. Filter tradeable symbols
 * 3. Enrich with intraday stats
 * 4. Enrich with context data
 * 5. Enrich with EOD stats (for swing/long-term)
 * 6. Rank and generate trade setups
 */
class RunAnalysisUseCase(
    private val repository: MarketDataRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    private val discoverCandidates = DiscoverCandidatesUseCase(repository)
    private val filterTradeable = FilterTradeableUseCase(repository)
    private val enrichIntraday = EnrichIntradayUseCase(repository)
    private val enrichContext = EnrichContextUseCase(repository)
    private val enrichEod = EnrichEodUseCase(repository)
    private val rankSetups = RankSetupsUseCase(clock)
    
    /**
     * Run the complete analysis pipeline.
     * 
     * @param intent Trading intent (DAY_TRADE, SWING, LONG_TERM)
     * @param risk User risk tolerance
     * @param customTickers User-supplied list of tickers
     * @return AnalysisResult with counts and trade setups
     */
    suspend fun execute(
        intent: TradingIntent,
        risk: com.polaralias.signalsynthesis.data.settings.RiskTolerance = com.polaralias.signalsynthesis.data.settings.RiskTolerance.MODERATE,
        assetClass: com.polaralias.signalsynthesis.data.settings.AssetClass = com.polaralias.signalsynthesis.data.settings.AssetClass.STOCKS,
        discoveryMode: com.polaralias.signalsynthesis.data.settings.DiscoveryMode = com.polaralias.signalsynthesis.data.settings.DiscoveryMode.STATIC,
        customTickers: List<String> = emptyList(),
        blocklist: List<String> = emptyList(),
        screenerThresholds: Map<String, Double> = emptyMap()
    ): AnalysisResult {
        // Step 1: Discover candidates
        val rawCandidateMap = discoverCandidates.execute(intent, risk, assetClass, discoveryMode, customTickers, screenerThresholds)
        
        // Filter out blocklisted tickers
        val candidateMap = rawCandidateMap.filter { (symbol, _) -> !blocklist.contains(symbol) }
        val symbols = candidateMap.keys.toList()
        
        if (symbols.isEmpty()) {
            return AnalysisResult(
                intent = intent,
                totalCandidates = 0,
                tradeableCount = 0,
                setupCount = 0,
                setups = emptyList(),
                generatedAt = java.time.Instant.now(clock)
            )
        }
        
        // Step 2: Filter tradeable
        val minPrice = if (risk == com.polaralias.signalsynthesis.data.settings.RiskTolerance.AGGRESSIVE) 0.1 else 1.0
        val tradeable = filterTradeable.execute(symbols, minPrice = minPrice)
        if (tradeable.isEmpty()) {
            return AnalysisResult(
                intent = intent,
                totalCandidates = symbols.size,
                tradeableCount = 0,
                setupCount = 0,
                setups = emptyList(),
                generatedAt = java.time.Instant.now(clock)
            )
        }
        
        // Step 3: Fetch quotes for ranking
        val quotes = repository.getQuotes(tradeable)
        
        // Step 4: Enrich with intraday stats
        val intradayStats = enrichIntraday.execute(tradeable, days = 2)
        
        // Step 5: Enrich with context data
        val contextData = enrichContext.execute(tradeable)
        
        // Step 6: Enrich with EOD stats (for swing and long-term only)
        val eodStats = if (intent != TradingIntent.DAY_TRADE) {
            enrichEod.execute(tradeable, days = 200)
        } else {
            emptyMap()
        }
        
        // Step 7: Rank and generate setups
        val rawSetups = rankSetups.execute(
            symbols = tradeable,
            quotes = quotes,
            intradayStats = intradayStats,
            eodStats = eodStats,
            contextData = contextData,
            intent = intent
        )

        // Enrich setups with source information
        val setups = rawSetups.map { it.copy(source = candidateMap[it.symbol] ?: com.polaralias.signalsynthesis.domain.model.TickerSource.PREDEFINED) }
        
        return AnalysisResult(
            intent = intent,
            totalCandidates = symbols.size,
            tradeableCount = tradeable.size,
            setupCount = setups.size,
            setups = setups,
            generatedAt = java.time.Instant.now(clock)
        )
    }
}
