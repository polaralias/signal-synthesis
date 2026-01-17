package com.polaralias.signalsynthesis.domain.provider

import com.polaralias.signalsynthesis.domain.model.DailyBar

interface DailyProvider {
    suspend fun getDaily(symbol: String, days: Int): List<DailyBar>
}
