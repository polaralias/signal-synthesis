package com.polaralias.signalsynthesis.domain.provider

import com.polaralias.signalsynthesis.domain.model.IntradayBar

interface IntradayProvider {
    suspend fun getIntraday(symbol: String, days: Int): List<IntradayBar>
}
