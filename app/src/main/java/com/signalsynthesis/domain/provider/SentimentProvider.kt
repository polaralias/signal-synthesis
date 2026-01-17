package com.signalsynthesis.domain.provider

import com.signalsynthesis.domain.model.SentimentData

interface SentimentProvider {
    suspend fun getSentiment(symbol: String): SentimentData?
}
