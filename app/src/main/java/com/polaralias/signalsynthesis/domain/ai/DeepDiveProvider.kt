package com.polaralias.signalsynthesis.domain.ai

import com.polaralias.signalsynthesis.domain.model.DeepDive
import com.polaralias.signalsynthesis.domain.model.RssHeadline
import com.polaralias.signalsynthesis.domain.model.TradingIntent

interface DeepDiveProvider {
    suspend fun deepDive(
        symbol: String,
        intent: TradingIntent,
        snapshot: String,
        rssHeadlines: List<RssHeadline>?,
        apiKey: String,
        model: String,
        timeoutMs: Long
    ): DeepDive
}
