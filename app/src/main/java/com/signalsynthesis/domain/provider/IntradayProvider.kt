package com.signalsynthesis.domain.provider

import com.signalsynthesis.domain.model.IntradayBar

interface IntradayProvider {
    suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar>
}
