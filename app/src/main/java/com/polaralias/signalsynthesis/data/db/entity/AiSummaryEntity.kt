package com.polaralias.signalsynthesis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_summaries")
data class AiSummaryEntity(
    @PrimaryKey val symbol: String,
    val summary: String,
    val risksJson: String,
    val verdict: String,
    val generatedAt: Long
)
