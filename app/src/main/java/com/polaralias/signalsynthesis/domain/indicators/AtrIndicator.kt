package com.polaralias.signalsynthesis.domain.indicators

import com.polaralias.signalsynthesis.domain.model.DailyBar
import com.polaralias.signalsynthesis.domain.model.IntradayBar
import kotlin.math.abs
import kotlin.math.max

/**
 * Calculates the Average True Range (ATR) indicator.
 * 
 * True Range = max(high - low, abs(high - prevClose), abs(low - prevClose))
 * ATR = Average of True Range over the period
 * 
 * Standard period is 14.
 */
object AtrIndicator {
    
    /**
     * Calculate ATR from intraday bars.
     * 
     * @param bars List of intraday bars (oldest first)
     * @param period ATR period (default 14)
     * @return ATR value, or null if insufficient data
     */
    fun calculateFromIntraday(bars: List<IntradayBar>, period: Int = 14): Double? {
        if (bars.size < period + 1) return null
        
        val trueRanges = mutableListOf<Double>()
        
        for (i in 1 until bars.size) {
            val current = bars[i]
            val previous = bars[i - 1]
            
            val tr = calculateTrueRange(
                high = current.high,
                low = current.low,
                previousClose = previous.close
            )
            trueRanges.add(tr)
        }
        
        if (trueRanges.size < period) return null
        
        // Calculate initial ATR (simple average)
        var atr = trueRanges.take(period).average()
        
        // Calculate smoothed ATR for remaining periods
        for (i in period until trueRanges.size) {
            atr = ((atr * (period - 1)) + trueRanges[i]) / period
        }
        
        return atr
    }
    
    /**
     * Calculate ATR from daily bars.
     * 
     * @param bars List of daily bars (oldest first)
     * @param period ATR period (default 14)
     * @return ATR value, or null if insufficient data
     */
    fun calculateFromDaily(bars: List<DailyBar>, period: Int = 14): Double? {
        if (bars.size < period + 1) return null
        
        val trueRanges = mutableListOf<Double>()
        
        for (i in 1 until bars.size) {
            val current = bars[i]
            val previous = bars[i - 1]
            
            val tr = calculateTrueRange(
                high = current.high,
                low = current.low,
                previousClose = previous.close
            )
            trueRanges.add(tr)
        }
        
        if (trueRanges.size < period) return null
        
        // Calculate initial ATR (simple average)
        var atr = trueRanges.take(period).average()
        
        // Calculate smoothed ATR for remaining periods
        for (i in period until trueRanges.size) {
            atr = ((atr * (period - 1)) + trueRanges[i]) / period
        }
        
        return atr
    }
    
    /**
     * Calculate the True Range for a single bar.
     * 
     * @param high High price
     * @param low Low price
     * @param previousClose Previous bar's close price
     * @return True Range value
     */
    private fun calculateTrueRange(high: Double, low: Double, previousClose: Double): Double {
        val range1 = high - low
        val range2 = abs(high - previousClose)
        val range3 = abs(low - previousClose)
        
        return max(range1, max(range2, range3))
    }
}
