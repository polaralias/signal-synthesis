package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupDetailScreen(
    uiState: AnalysisUiState,
    symbol: String,
    onBack: () -> Unit,
    onRequestSummary: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit
) {
    val setup = uiState.result?.setups?.firstOrNull { it.symbol == symbol }
    var showRawData by remember { mutableStateOf(false) }
    val aiSummary = uiState.aiSummaries[symbol]

    LaunchedEffect(symbol, uiState.hasLlmKey) {
        if (uiState.hasLlmKey) {
            onRequestSummary(symbol)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(symbol) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleWatchlist(symbol) }) {
                        Text(if (uiState.watchlist.contains(symbol)) "⭐" else "☆")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (setup == null) {
                Text("Setup not found.")
                return@Scaffold
            }

            SectionHeader("AI Summary")
            when {
                !uiState.hasLlmKey -> Text("Add an LLM key to enable AI synthesis.")
                aiSummary?.status == AiSummaryStatus.LOADING -> Text("Generating AI synthesis...")
                aiSummary?.status == AiSummaryStatus.ERROR -> Text(aiSummary.errorMessage ?: "AI synthesis failed.")
                aiSummary?.status == AiSummaryStatus.READY -> {
                    Text(aiSummary.summary.orEmpty())
                    if (aiSummary.risks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader("Risks")
                        aiSummary.risks.forEach { risk ->
                            Text("- $risk")
                        }
                    }
                    aiSummary.verdict?.let { verdict ->
                        if (verdict.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader("Verdict")
                            Text(verdict)
                        }
                    }
                }
                else -> Text("AI synthesis will appear once generated.")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { showRawData = !showRawData }) {
                Text(if (showRawData) "Hide Raw Data" else "View Raw Data")
            }

            if (showRawData) {
                SectionHeader("Setup")
                Text("Type: ${setup.setupType}")
                Text("Intent: ${setup.intent}")
                Text("Trigger: ${formatPrice(setup.triggerPrice)}")
                Text("Stop: ${formatPrice(setup.stopLoss)}")
                Text("Target: ${formatPrice(setup.targetPrice)}")
                Text("Confidence: ${formatPercent(setup.confidence)}")
                Text("Valid until: ${formatValidUntil(setup.validUntil)}")
                SectionHeader("Reasons")
                if (setup.reasons.isEmpty()) {
                    Text("No reasons recorded.")
                } else {
                    setup.reasons.forEach { reason ->
                        Text("• $reason")
                    }
                }
            }
        }
    }
}

private fun formatValidUntil(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
