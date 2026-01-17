package com.signalsynthesis.domain.indicators

import com.signalsynthesis.domain.model.IntradayBar

/**
 * Calculates the Volume Weighted Average Price (VWAP) from intraday bars.
 * 
 * VWAP = sum(price * volume) / sum(volume)
 * where price is typically the typical price: (high + low + close) / 3
 */
object VwapIndicator {
    
    /**
     * Calculate VWAP from a list of intraday bars.
     * 
     * @param bars List of intraday bars, typically for a single trading day
     * @return VWAP value, or null if bars are empty or total volume is zero
     */
    fun calculate(bars: List<IntradayBar>): Double? {
        if (bars.isEmpty()) return null
        
        var sumPriceVolume = 0.0
        var sumVolume = 0L
        
        for (bar in bars) {
            // Use typical price (HLC/3)
            val typicalPrice = (bar.high + bar.low + bar.close) / 3.0
            sumPriceVolume += typicalPrice * bar.volume
            sumVolume += bar.volume
        }
        
        if (sumVolume == 0L) return null
        
        return sumPriceVolume / sumVolume
    }
    
    /**
     * Calculate VWAP using closing prices instead of typical prices.
     * 
     * @param bars List of intraday bars
     * @return VWAP value, or null if bars are empty or total volume is zero
     */
    fun calculateFromClose(bars: List<IntradayBar>): Double? {
        if (bars.isEmpty()) return null
        
        var sumPriceVolume = 0.0
        var sumVolume = 0L
        
        for (bar in bars) {
            sumPriceVolume += bar.close * bar.volume
            sumVolume += bar.volume
        }
        
        if (sumVolume == 0L) return null
        
        return sumPriceVolume / sumVolume
    }
}
