package com.polaralias.signalsynthesis.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polaralias.signalsynthesis.util.*
import java.time.*
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onBack: () -> Unit
) {
    val activities by com.polaralias.signalsynthesis.util.ActivityLogger.activities.collectAsState()

    AmbientBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        RainbowMcpText(
                            text = "SYSTEM TELEMETRY", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { com.polaralias.signalsynthesis.util.ActivityLogger.clear() }) {
                            Text("FLUSH", fontWeight = FontWeight.Bold, color = com.polaralias.signalsynthesis.ui.theme.NeonRed)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
        if (activities.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No activity recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activities) { entry ->
                    ActivityItem(entry)
                }
            }
        }
    }
}
}

@Composable
private fun ActivityItem(entry: com.polaralias.signalsynthesis.util.ActivityEntry) {
    val accentColor = when (entry.type) {
        com.polaralias.signalsynthesis.util.ActivityType.API_REQUEST -> com.polaralias.signalsynthesis.ui.theme.NeonBlue
        com.polaralias.signalsynthesis.util.ActivityType.LLM_REQUEST -> com.polaralias.signalsynthesis.ui.theme.NeonPurple
    }

    com.polaralias.signalsynthesis.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (entry.type == com.polaralias.signalsynthesis.util.ActivityType.API_REQUEST) "DATA" else "NEURAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isSuccess) MaterialTheme.colorScheme.onSurface else com.polaralias.signalsynthesis.ui.theme.NeonRed
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            
            Text("INPUT_STREAM:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Text(
                text = entry.input,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 3,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            
            Text("OUTPUT_BUFFER:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Text(
                text = entry.output,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 5,
                color = if (entry.isSuccess) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f) else com.polaralias.signalsynthesis.ui.theme.NeonRed
            )

            if (entry.durationMs > 0) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LATENCY: ${entry.durationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTimestamp(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}

@Composable
fun Box(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}
