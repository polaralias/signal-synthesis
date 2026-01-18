package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polaralias.signalsynthesis.domain.model.AnalysisResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: AnalysisUiState,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onViewResult: (AnalysisResult) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    if (uiState.history.isNotEmpty()) {
                        Button(onClick = onClearHistory) {
                            Text("Clear")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.history.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text("No analysis history found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.history) { result ->
                    HistoryItem(
                        result = result,
                        onClick = { onViewResult(result) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    result: AnalysisResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.intent.name.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatTime(result.generatedAt),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "${result.totalCandidates} candidates analyzed. ${result.setupCount} setups found.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
