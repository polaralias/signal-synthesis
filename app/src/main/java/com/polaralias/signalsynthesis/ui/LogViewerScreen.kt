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
import com.polaralias.signalsynthesis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    onBack: () -> Unit
) {
    val activities by com.polaralias.signalsynthesis.util.ActivityLogger.activities.collectAsState()

    AmbientBackground {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandPrimary)
                        }
                        
                        RainbowMcpText(
                            text = "TELEMETRY",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                        )

                        IconButton(onClick = { com.polaralias.signalsynthesis.util.ActivityLogger.clear() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Flush", tint = ErrorRed.copy(alpha = 0.6f))
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                AppHeader(
                    title = "TRAFFIC",
                    subtitle = "Real-time protocol feedback"
                )

                if (activities.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Terminal, 
                                contentDescription = null, 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "NO ACTIVITY RECORDED", 
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(activities) { entry ->
                            ActivityItem(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(entry: com.polaralias.signalsynthesis.util.ActivityEntry) {
    val accentColor = when (entry.type) {
        com.polaralias.signalsynthesis.util.ActivityType.API_REQUEST -> BrandPrimary
        com.polaralias.signalsynthesis.util.ActivityType.LLM_REQUEST -> BrandSecondary
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (entry.type == com.polaralias.signalsynthesis.util.ActivityType.API_REQUEST) "DATA" else "NEURAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = if (entry.isSuccess) MaterialTheme.colorScheme.onSurface else ErrorRed
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            
            LogCodeSection(
                label = "INPUT_STREAM",
                content = entry.input,
                maxLines = 3
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            
            LogCodeSection(
                label = "OUTPUT_BUFFER",
                content = entry.output,
                maxLines = 5,
                color = if (entry.isSuccess) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else ErrorRed.copy(alpha = 0.8f)
            )

            if (entry.durationMs > 0) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(12.dp), tint = accentColor.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${entry.durationMs}MS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = accentColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LogCodeSection(label: String, content: String, maxLines: Int, color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) {
    Column {
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), 
            fontWeight = FontWeight.Black, 
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(8.dp)
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                maxLines = maxLines,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = color
            )
        }
    }
}

private fun formatTimestamp(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}

