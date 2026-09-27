package com.naveen.callqueue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naveen.callqueue.telecom.SimOption
import com.naveen.callqueue.ui.theme.BrandBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sims: List<SimOption>,
    selectedSimId: String?,
    onSelectSim: (SimOption) -> Unit,
    retryMax: Int,
    onRetryMaxChange: (Int) -> Unit,
    outcomeTaggingEnabled: Boolean,
    onOutcomeTaggingChange: (Boolean) -> Unit,
    gapSeconds: Int,
    onGapChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            SettingsSection(title = "Calling SIM") {
                if (sims.isEmpty()) {
                    Text(
                        "No SIMs detected yet — grant the Phone permission first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                        Column {
                            sims.forEachIndexed { index, sim ->
                                if (index > 0) {
                                    androidx.compose.material3.HorizontalDivider()
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = sim.handle.id == selectedSimId,
                                            onClick = { onSelectSim(sim) }
                                        )
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.SimCard, contentDescription = null, tint = BrandBlue)
                                    Text(
                                        sim.label,
                                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    RadioButton(selected = sim.handle.id == selectedSimId, onClick = { onSelectSim(sim) })
                                }
                            }
                        }
                    }
                    Text(
                        "Auto-calls always go out on this SIM — no \"choose SIM\" popup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }
            }

            SettingsSection(title = "If they don't pick up") {
                Text(
                    "Total calls to a number that doesn't pick up, before moving on (Twice = call, then call again once)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    listOf(1 to "Once", 2 to "Twice", 3 to "Thrice").forEachIndexed { index, (count, label) ->
                        SegmentedButton(
                            selected = retryMax == count,
                            onClick = { onRetryMaxChange(count) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            SettingsSection(title = "Outcome tags") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Prompt to tag each call (Interested / No Answer / etc.)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = outcomeTaggingEnabled, onCheckedChange = onOutcomeTaggingChange)
                }
            }

            SettingsSection(title = "Wait between calls") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(2, 4, 6, 10, 20).forEachIndexed { index, seconds ->
                        SegmentedButton(
                            selected = seconds == gapSeconds,
                            onClick = { onGapChange(seconds) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 5)
                        ) {
                            Text("${seconds}s")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}
