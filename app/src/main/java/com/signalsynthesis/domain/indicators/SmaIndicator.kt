package com.signalsynthesis.domain.indicators

import com.signalsynthesis.domain.model.DailyBar

/**
 * Calculates the Simple Moving Average (SMA) indicator.
 * 
 * SMA = sum of closing prices over period / period
 * 
 * Common periods: 50, 200
 */
object SmaIndicator {
    
    /**
     * Calculate SMA from a list of closing prices.
     * 
     * @param closePrices List of closing prices (oldest first)
     * @param period SMA period
     * @return SMA value, or null if insufficient data
     */
    fun calculate(closePrices: List<Double>, period: Int): Double? {
        if (closePrices.size < period) return null
        
        // Take the most recent 'period' prices
        val recentPrices = closePrices.takeLast(period)
        return recentPrices.average()
    }
    
    /**
     * Calculate SMA from daily bars.
     * 
     * @param bars List of daily bars (oldest first)
     * @param period SMA period
     * @return SMA value, or null if insufficient data
     */
    fun calculateFromDaily(bars: List<DailyBar>, period: Int): Double? {
        val closePrices = bars.map { it.close }
        return calculate(closePrices, period)
    }
    
    /**
     * Calculate multiple SMAs at once.
     * 
     * @param bars List of daily bars (oldest first)
     * @param periods List of SMA periods to calculate
     * @return Map of period to SMA value
     */
    fun calculateMultiple(bars: List<DailyBar>, periods: List<Int>): Map<Int, Double?> {
        val closePrices = bars.map { it.close }
        return periods.associateWith { period ->
            calculate(closePrices, period)
        }
    }
}
