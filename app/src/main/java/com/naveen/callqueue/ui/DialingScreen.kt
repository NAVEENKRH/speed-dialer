package com.naveen.callqueue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naveen.callqueue.data.CallOutcomes
import com.naveen.callqueue.data.EntryStatus
import com.naveen.callqueue.service.DialSessionState
import com.naveen.callqueue.service.SessionPhase
import com.naveen.callqueue.ui.theme.BrandBlue
import com.naveen.callqueue.ui.theme.BrandBlueLight
import com.naveen.callqueue.ui.theme.SuccessGreen
import com.naveen.callqueue.ui.theme.SuccessGreenLight
import com.naveen.callqueue.ui.theme.WarnAmber
import com.naveen.callqueue.ui.theme.WarnAmberLight

private data class PhaseStyle(val label: String, val color: Color, val bg: Color)

private fun phaseStyle(phase: SessionPhase): PhaseStyle = when (phase) {
    SessionPhase.DIALING -> PhaseStyle("Dialing…", BrandBlue, BrandBlueLight)
    SessionPhase.ON_CALL -> PhaseStyle("On call", SuccessGreen, SuccessGreenLight)
    SessionPhase.AWAITING_TAG -> PhaseStyle("Call ended", BrandBlue, BrandBlueLight)
    SessionPhase.HELD -> PhaseStyle("Held", WarnAmber, WarnAmberLight)
    SessionPhase.FINISHED -> PhaseStyle("Finished", SuccessGreen, SuccessGreenLight)
    SessionPhase.IDLE -> PhaseStyle("Idle", Color.Gray, Color.LightGray)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialingScreen(
    state: DialSessionState,
    onHold: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onLater: () -> Unit,
    onTag: (String) -> Unit,
    onRedialNow: () -> Unit,
    onEndSession: () -> Unit
) {
    val style = phaseStyle(state.phase)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.listName, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onEndSession) {
                        Text("End", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = if (state.totalCount == 0) 0f else state.doneCount / state.totalCount.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${state.doneCount} done · ${state.waitingCount} left",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text("of ${state.totalCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (state.phase == SessionPhase.FINISHED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreenLight)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("All done!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        Text("You worked through the whole list.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                val current = state.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier.size(56.dp).background(style.bg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = style.color, modifier = Modifier.size(28.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    current?.name ?: "No name yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(current?.number ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier.background(style.bg, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(style.label, color = style.color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(state.statusMessage, style = MaterialTheme.typography.bodyMedium)
                        state.countdownSeconds?.let {
                            Text(
                                "Next in ${it}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                if (state.phase == SessionPhase.AWAITING_TAG) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.canRedialCurrent) {
                            Button(
                                onClick = onRedialNow,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(
                                    "  Call again" + (state.countdownSeconds?.let { " · ${it}s" } ?: ""),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        if (state.outcomeTaggingEnabled) {
                            Text("How did it go?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CallOutcomes.OPTIONS) { outcome ->
                                    AssistChip(
                                        onClick = { onTag(outcome) },
                                        label = { Text(outcome) },
                                        shape = RoundedCornerShape(50),
                                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (state.phase == SessionPhase.HELD) {
                        Button(
                            onClick = onResume,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Resume")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onHold,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarnAmber)
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Hold")
                        }
                    }
                    OutlinedButton(
                        onClick = onLater,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Later")
                    }
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Skip")
                    }
                }
            }

            Text("Queue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.queue, key = { it.id }) { entry ->
                    val label = entry.number.takeLast(5)
                    val bg = when (entry.status) {
                        EntryStatus.DONE -> SuccessGreenLight
                        EntryStatus.SKIPPED -> MaterialTheme.colorScheme.errorContainer
                        EntryStatus.CALLING -> BrandBlueLight
                        EntryStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val fg = when (entry.status) {
                        EntryStatus.DONE -> SuccessGreen
                        EntryStatus.SKIPPED -> MaterialTheme.colorScheme.error
                        EntryStatus.CALLING -> BrandBlue
                        EntryStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Card(
                        modifier = Modifier.wrapContentWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = bg)
                    ) {
                        Text(
                            "…$label",
                            color = fg,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
