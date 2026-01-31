package com.polaralias.signalsynthesis.domain.model

data class AiSummaryCacheKey(
    val model: String,
    val promptHash: String
)
