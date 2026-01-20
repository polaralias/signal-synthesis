package com.polaralias.signalsynthesis.domain.usecase

import com.polaralias.signalsynthesis.domain.model.CompanyProfile
import com.polaralias.signalsynthesis.domain.model.EodStats
import com.polaralias.signalsynthesis.domain.model.FinancialMetrics
import com.polaralias.signalsynthesis.domain.model.IntradayStats
import com.polaralias.signalsynthesis.domain.model.Quote
import com.polaralias.signalsynthesis.domain.model.SentimentData
import com.polaralias.signalsynthesis.domain.model.TradeSetup
import com.polaralias.signalsynthesis.domain.model.TradingIntent
import com.polaralias.signalsynthesis.domain.usecase.SymbolContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Ranks and scores symbols to generate trade setups.
 * 
 * Scoring heuristics (matching MCP server logic):
 * - Price above VWAP: +1
 * - RSI < 30 (oversold): +1
 * - RSI > 70 (overbought): -0.5
 * - Price above SMA-200: +1
 * - Sentiment score > 0.2: +1
 * 
 * Setup type determination:
 * - Score > 2.0: High Probability
 * - Otherwise: Speculative
 * 
 * Price levels:
 * - Trigger: current price
 * - Stop loss: price * 0.98
 * - Target: price * 1.05
 */
class RankSetupsUseCase(
    private val clock: Clock = Clock.systemUTC()
) {
    
    /**
     * Rank symbols and generate trade setups.
     * 
     * @param symbols List of symbols to rank
     * @param quotes Map of symbol to Quote
     * @param intradayStats Map of symbol to IntradayStats
     * @param eodStats Map of symbol to EodStats (may be empty for day trading)
     * @param sentimentScores Map of symbol to sentiment score
     * @param intent Trading intent
     * @return List of TradeSetup sorted by confidence (descending)
     */
    fun execute(
        symbols: List<String>,
        quotes: Map<String, Quote>,
        intradayStats: Map<String, IntradayStats>,
        eodStats: Map<String, EodStats>,
        contextData: Map<String, SymbolContext>,
        intent: TradingIntent,
        customTickers: List<String> = emptyList()
    ): List<TradeSetup> {
        if (symbols.isEmpty()) return emptyList()
        
        val setups = symbols.mapNotNull { symbol ->
            val quote = quotes[symbol] ?: return@mapNotNull null
            val stats = intradayStats[symbol]
            val eod = eodStats[symbol]
            val context = contextData[symbol]
            
            scoreSymbol(symbol, quote, stats, eod, context, intent, symbol in customTickers)
        }
        
        // Sort by confidence descending
        return setups.sortedByDescending { it.confidence }
    }
    
    private fun scoreSymbol(
        symbol: String,
        quote: Quote,
        intradayStats: IntradayStats?,
        eodStats: EodStats?,
        context: SymbolContext?,
        intent: TradingIntent,
        isUserAdded: Boolean = false
    ): TradeSetup? {
        val price = quote.price
        val sentimentScore = context?.sentiment?.score
        var score = 0.0
        val reasons = mutableListOf<String>()
        
        // Check price vs VWAP
        val vwap = intradayStats?.vwap
        if (vwap != null && price > vwap) {
            score += 1.0
            reasons.add("Price above VWAP (${String.format("%.2f", vwap)})")
        }
        
        // Check RSI
        val rsi = intradayStats?.rsi14
        if (rsi != null) {
            when {
                rsi < 30 -> {
                    score += 1.0
                    reasons.add("RSI oversold (${String.format("%.1f", rsi)})")
                }
                rsi > 70 -> {
                    score -= 0.5
                    reasons.add("RSI overbought (${String.format("%.1f", rsi)})")
                }
                else -> {
                    reasons.add("RSI neutral (${String.format("%.1f", rsi)})")
                }
            }
        }
        
        // Check price vs SMA-200 (for swing and long-term)
        val sma200 = eodStats?.sma200
        if (sma200 != null && price > sma200) {
            score += 1.0
            reasons.add("Price above SMA-200 (${String.format("%.2f", sma200)})")
        }
        
        // Check sentiment
        if (sentimentScore != null && sentimentScore > 0.2) {
            score += 1.0
            val label = when {
                sentimentScore > 0.2 -> "Bullish"
                sentimentScore < -0.2 -> "Bearish"
                else -> "Neutral"
            }
            reasons.add("Positive sentiment ($label, ${String.format("%.2f", sentimentScore)})")
        }

        // Check for upcoming earnings
        var earningsPenalty = 0.0
        val earningsDateStr = context?.metrics?.earningsDate
        if (earningsDateStr != null) {
            try {
                // FMP format is typically "yyyy-MM-dd HH:mm:ss" or "yyyy-MM-dd"
                val datePart = earningsDateStr.take(10)
                val earningsDate = LocalDate.parse(datePart)
                val today = LocalDate.now(clock)
                val daysUntil = ChronoUnit.DAYS.between(today, earningsDate)
                
                if (daysUntil in 0..3) {
                    val severity = when (intent) {
                        TradingIntent.DAY_TRADE -> 0.2 // Minor risk for day traders
                        TradingIntent.SWING -> 0.8 // Major risk for swing traders
                        TradingIntent.LONG_TERM -> 0.4 // Moderate risk for long term
                    }
                    earningsPenalty = severity
                    reasons.add("Upcoming earnings in $daysUntil days (High Volatility Risk)")
                }
            } catch (_: Exception) {
                // Skip if date format is unexpected
            }
        }
        
        // Calculate confidence (normalized score)
        val maxScore = 4.0 // Maximum possible score
        val rawConfidence = score / maxScore
        val confidence = min(1.0, max(0.1, rawConfidence - earningsPenalty))
        
        // Determine setup type
        val setupType = if (score > 2.0) "High Probability" else "Speculative"
        
        // Calculate price levels
        val triggerPrice = price
        val stopLoss = price * 0.98
        val targetPrice = price * 1.05
        
        // Calculate validity duration based on intent
        val validUntil = when (intent) {
            TradingIntent.DAY_TRADE -> Instant.now(clock).plusSeconds(30 * 60) // 30 minutes
            TradingIntent.SWING -> Instant.now(clock).plusSeconds(24 * 60 * 60) // 1 day
            TradingIntent.LONG_TERM -> Instant.now(clock).plusSeconds(7 * 24 * 60 * 60) // 1 week
        }
        
        return TradeSetup(
            symbol = symbol,
            setupType = setupType,
            triggerPrice = triggerPrice,
            stopLoss = stopLoss,
            targetPrice = targetPrice,
            confidence = confidence,
            reasons = reasons,
            validUntil = validUntil,
            intent = intent,
            intradayStats = intradayStats,
            eodStats = eodStats,
            profile = context?.profile,
            metrics = context?.metrics,
            sentiment = context?.sentiment,
            isUserAdded = isUserAdded
        )
    }
}
