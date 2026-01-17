package com.signalsynthesis.domain.provider

import com.signalsynthesis.domain.model.DailyBar

interface DailyProvider {
    suspend fun getDaily(symbol: String, days: Int): List<DailyBar>
}
