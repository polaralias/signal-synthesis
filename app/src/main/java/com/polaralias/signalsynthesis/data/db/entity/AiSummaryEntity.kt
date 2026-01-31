package com.polaralias.signalsynthesis.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "ai_summaries",
    primaryKeys = ["symbol", "model", "promptHash"]
)
data class AiSummaryEntity(
    val symbol: String,
    val model: String,
    val promptHash: String,
    val summary: String,
    val risksJson: String,
    val verdict: String,
    val generatedAt: Long
)
