package com.polaralias.signalsynthesis.domain.model

data class AiSynthesis(
    val summary: String,
    val risks: List<String>,
    val verdict: String
)
