package com.polaralias.signalsynthesis.domain.provider

import com.polaralias.signalsynthesis.domain.model.SentimentData

interface SentimentProvider {
    suspend fun getSentiment(symbol: String): SentimentData?
}
