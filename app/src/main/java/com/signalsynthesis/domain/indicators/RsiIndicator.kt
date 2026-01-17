package com.signalsynthesis.domain.indicators

import com.signalsynthesis.domain.model.DailyBar
import com.signalsynthesis.domain.model.IntradayBar
import kotlin.math.abs

/**
 * Calculates the Relative Strength Index (RSI) indicator.
 * 
 * RSI = 100 - (100 / (1 + RS))
 * where RS = Average Gain / Average Loss over the period
 * 
 * Standard period is 14.
 */
object RsiIndicator {
    
    /**
     * Calculate RSI from a list of closing prices.
     * 
     * @param closePrices List of closing prices (oldest first)
     * @param period RSI period (default 14)
     * @return RSI value (0-100), or null if insufficient data
     */
    fun calculate(closePrices: List<Double>, period: Int = 14): Double? {
        if (closePrices.size < period + 1) return null
        
        // Calculate price changes
        val changes = mutableListOf<Double>()
        for (i in 1 until closePrices.size) {
            changes.add(closePrices[i] - closePrices[i - 1])
        }
        
        if (changes.size < period) return null
        
        // Calculate initial average gain and loss
        var avgGain = 0.0
        var avgLoss = 0.0
        
        for (i in 0 until period) {
            val change = changes[i]
            if (change > 0) {
                avgGain += change
            } else {
                avgLoss += abs(change)
            }
        }
        
        avgGain /= period
        avgLoss /= period
        
        // Calculate smoothed averages for remaining periods
        for (i in period until changes.size) {
            val change = changes[i]
            if (change > 0) {
                avgGain = ((avgGain * (period - 1)) + change) / period
                avgLoss = (avgLoss * (period - 1)) / period
            } else {
                avgGain = (avgGain * (period - 1)) / period
                avgLoss = ((avgLoss * (period - 1)) + abs(change)) / period
            }
        }
        
        // Avoid division by zero
        if (avgLoss == 0.0) {
            return if (avgGain == 0.0) 50.0 else 100.0
        }
        
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
    
    /**
     * Calculate RSI from intraday bars.
     * 
     * @param bars List of intraday bars (oldest first)
     * @param period RSI period (default 14)
     * @return RSI value, or null if insufficient data
     */
    fun calculateFromIntraday(bars: List<IntradayBar>, period: Int = 14): Double? {
        val closePrices = bars.map { it.close }
        return calculate(closePrices, period)
    }
    
    /**
     * Calculate RSI from daily bars.
     * 
     * @param bars List of daily bars (oldest first)
     * @param period RSI period (default 14)
     * @return RSI value, or null if insufficient data
     */
    fun calculateFromDaily(bars: List<DailyBar>, period: Int = 14): Double? {
        val closePrices = bars.map { it.close }
        return calculate(closePrices, period)
    }
}
