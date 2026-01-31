package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.data.repository.MarketDataRepository
import com.polaralias.signalsynthesis.domain.model.AnalysisResult
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.data.settings.RiskTolerance
import com.polaralias.signalsynthesis.data.settings.AssetClass
import com.polaralias.signalsynthesis.data.settings.DiscoveryMode
import com.polaralias.signalsynthesis.domain.model.TickerSource
import com.polaralias.signalsynthesis.util.Logger
import java.time.Clock
import java.time.Instant

/**
 * Staged orchestration use case (V2) that implements the LLM shortlist gate.
 * 
 * Flow:
 * 1. API Discovery (symbols)
 * 2. Filter Tradeable
 * 3. Fetch Quotes (lightweight)
 * 4. LLM Shortlist (Gate) -> identifies high-potential symbols
 * 5. Targeted Enrichment -> only for shortlisted symbols
 * 6. Rank and Generate Setups
 * 7. Decision Update (Keep/Drop + Bias)
 * 8. Fundamentals + News Synthesis
 */
class RunAnalysisV2UseCase(
    private val repository: MarketDataRepository,
    private val stageModelRouter: com.polaralias.signalsynthesis.domain.ai.StageModelRouter,
    private val rssDigestBuilder: BuildRssDigestUseCase? = null,
    private val clock: Clock = Clock.systemUTC()
) {
    private val discoverCandidates = DiscoverCandidatesUseCase(repository)
    private val filterTradeable = FilterTradeableUseCase(repository)
    private val shortlistCandidates = ShortlistCandidatesUseCase(stageModelRouter)
    private val enrichIntraday = EnrichIntradayUseCase(repository)
    private val enrichContext = EnrichContextUseCase(repository)
    private val enrichEod = EnrichEodUseCase(repository)
    private val rankSetups = RankSetupsUseCase(clock)
    private val updateDecisions = UpdateDecisionsUseCase(stageModelRouter)
    private val synthesizeFundamentalsAndNews = SynthesizeFundamentalsAndNewsUseCase(stageModelRouter)
    
    /**
     * Executes the staged analysis pipeline.
     */
    suspend fun execute(
        intent: TradingIntent,
        risk: RiskTolerance = RiskTolerance.MODERATE,
        assetClass: AssetClass = AssetClass.STOCKS,
        discoveryMode: DiscoveryMode = DiscoveryMode.CURATED,
        llmKey: String,
        customTickers: List<String> = emptyList(),
        blocklist: List<String> = emptyList(),
        screenerThresholds: Map<String, Double> = emptyMap(),
        maxShortlist: Int = 15,
        maxDecisionKeep: Int = 10,
        rssFeeds: List<String> = emptyList(),
        onProgress: ((String) -> Unit)? = null
    ): AnalysisResult {
        if (llmKey.isBlank()) {
            throw IllegalArgumentException("LLM key required for staged analysis.")
        }

        // Step 1: Discover candidates
        onProgress?.invoke("Discovering candidates...")
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
                generatedAt = Instant.now(clock)
            )
        }
        
        // Step 2: Filter tradeable
        onProgress?.invoke("Filtering tradeable symbols...")
        val minPrice = if (risk == RiskTolerance.AGGRESSIVE) 0.1 else 1.0
        val tradeable = filterTradeable.execute(symbols, minPrice = minPrice)
        if (tradeable.isEmpty()) {
            return AnalysisResult(
                intent = intent,
                totalCandidates = symbols.size,
                tradeableCount = 0,
                setupCount = 0,
                setups = emptyList(),
                generatedAt = Instant.now(clock)
            )
        }
        
        // Step 3: Fetch quotes for the shortlist stage
        onProgress?.invoke("Fetching quotes...")
        val quotes = repository.getQuotes(tradeable)
        
        // Step 4: LLM Shortlist Gate
        onProgress?.invoke("LLM Shortlisting (Gate)...")
        val shortlistPlan = shortlistCandidates.execute(
            symbols = tradeable,
            quotes = quotes,
            intent = intent,
            risk = risk,
            maxShortlist = maxShortlist
        )
        
        val shortlistedSymbols = shortlistPlan.shortlist
            .filter { !it.avoid }
            .map { it.symbol }
            .filter { tradeable.contains(it) } // Double check it was in our universe

        if (shortlistedSymbols.isEmpty()) {
            Logger.i("RunAnalysisV2", "Shortlist is empty, no symbols to enrich.")
            return AnalysisResult(
                intent = intent,
                totalCandidates = symbols.size,
                tradeableCount = tradeable.size,
                setupCount = 0,
                setups = emptyList(),
                generatedAt = Instant.now(clock),
                globalNotes = shortlistPlan.globalNotes
            )
        }

        Logger.i("RunAnalysisV2", "Proceeding with enrichment for ${shortlistedSymbols.size} symbols: $shortlistedSymbols")

        // Step 5: Targeted enrichment (ONLY for shortlisted symbols that requested it)
        onProgress?.invoke("Targeted enrichment (${shortlistedSymbols.size} symbols)...")
        
        val symbolsRequestingIntraday = shortlistPlan.shortlist
            .filter { it.requestedEnrichment.contains("INTRADAY") && shortlistedSymbols.contains(it.symbol) }
            .map { it.symbol }
            
        val symbolsRequestingEod = shortlistPlan.shortlist
            .filter { it.requestedEnrichment.contains("EOD") && shortlistedSymbols.contains(it.symbol) }
            .map { it.symbol }
            
        val symbolsRequestingContext = shortlistPlan.shortlist
            .filter { (it.requestedEnrichment.contains("FUNDAMENTALS") || it.requestedEnrichment.contains("SENTIMENT")) && shortlistedSymbols.contains(it.symbol) }
            .map { it.symbol }

        // If a symbol didn't request anything specific but is shortlisted, we run default enrichment based on intent
        val intradayStats = if (symbolsRequestingIntraday.isNotEmpty()) {
            enrichIntraday.execute(symbolsRequestingIntraday, days = 2)
        } else {
            enrichIntraday.execute(shortlistedSymbols, days = 2)
        }

        val contextData = if (symbolsRequestingContext.isNotEmpty()) {
            enrichContext.execute(symbolsRequestingContext)
        } else {
            enrichContext.execute(shortlistedSymbols)
        }

        val eodStats = when {
            symbolsRequestingEod.isNotEmpty() -> enrichEod.execute(symbolsRequestingEod, days = 200)
            intent != TradingIntent.DAY_TRADE -> enrichEod.execute(shortlistedSymbols, days = 200)
            else -> emptyMap()
        }
        
        // Step 6: Rank and generate setups
        onProgress?.invoke("Ranking setups...")
        val rawSetups = rankSetups.execute(
            symbols = shortlistedSymbols,
            quotes = quotes,
            intradayStats = intradayStats,
            eodStats = eodStats,
            contextData = contextData,
            intent = intent
        )

        // Enrich setups with source information
        val setupsWithSource = rawSetups.map { it.copy(source = candidateMap[it.symbol] ?: TickerSource.PREDEFINED) }

        // Step 7: Decision Update (keep/drop + bias)
        onProgress?.invoke("Updating decisions...")
        val decisionUpdate = updateDecisions.execute(
            setups = setupsWithSource,
            intent = intent,
            risk = risk,
            maxKeep = maxDecisionKeep
        )

        val keepSymbols = decisionUpdate.keep.map { it.symbol }.filter { it.isNotBlank() }.toSet()
        val dropSymbols = decisionUpdate.drop.map { it.symbol }.filter { it.isNotBlank() }.toSet()
        val filteredSetups = when {
            keepSymbols.isNotEmpty() -> setupsWithSource.filter { keepSymbols.contains(it.symbol) }
            dropSymbols.isNotEmpty() -> setupsWithSource.filterNot { dropSymbols.contains(it.symbol) }
            else -> setupsWithSource
        }

        val decisionMap = decisionUpdate.keep.associateBy { it.symbol }
        val setups = filteredSetups.map { setup ->
            val decision = decisionMap[setup.symbol]
            if (decision != null) {
                setup.copy(
                    setupBias = decision.setupBias,
                    mustReview = decision.mustReview,
                    rssNeeded = decision.rssNeeded,
                    decisionConfidence = if (decision.confidence > 0.0) decision.confidence else null
                )
            } else {
                setup
            }
        }

        if (setups.isEmpty()) {
            return AnalysisResult(
                intent = intent,
                totalCandidates = symbols.size,
                tradeableCount = tradeable.size,
                setupCount = 0,
                setups = emptyList(),
                generatedAt = Instant.now(clock),
                globalNotes = shortlistPlan.globalNotes,
                decisionUpdate = decisionUpdate
            )
        }
        
        // Step 8: Build RSS Digest for final setups
        val rssDigest = if (rssDigestBuilder != null && rssFeeds.isNotEmpty() && setups.isNotEmpty()) {
            onProgress?.invoke("Building RSS digest...")
            try {
                val rssTargets = setups.filter { it.rssNeeded }.map { it.symbol }.ifEmpty { setups.map { it.symbol } }
                rssDigestBuilder.execute(
                    tickers = rssTargets,
                    feedUrls = rssFeeds
                )
            } catch (e: Exception) {
                Logger.e("RunAnalysisV2", "RSS digest failed", e)
                null
            }
        } else null

        // Step 9: Fundamentals + news synthesis (optional)
        val fundamentalsNewsSynthesis = if (setups.isNotEmpty()) {
            onProgress?.invoke("Synthesizing fundamentals and news...")
            try {
                synthesizeFundamentalsAndNews.execute(
                    setups = setups,
                    rssDigest = rssDigest,
                    intent = intent,
                    risk = risk
                )
            } catch (e: Exception) {
                Logger.e("RunAnalysisV2", "Fundamentals/news synthesis failed", e)
                null
            }
        } else null
        
        return AnalysisResult(
            intent = intent,
            totalCandidates = symbols.size,
            tradeableCount = tradeable.size,
            setupCount = setups.size,
            setups = setups,
            generatedAt = Instant.now(clock),
            globalNotes = shortlistPlan.globalNotes,
            rssDigest = rssDigest,
            decisionUpdate = decisionUpdate,
            fundamentalsNewsSynthesis = fundamentalsNewsSynthesis
        )
    }
}
