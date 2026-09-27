package com.naveen.callqueue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naveen.callqueue.data.NumberParser
import com.naveen.callqueue.ui.theme.SuccessGreen
import com.naveen.callqueue.ui.theme.SuccessGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteNumbersScreen(
    initialText: String,
    onContinue: (String, List<NumberParser.ParsedNumber>) -> Unit,
    onOpenSettings: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val clipboard = LocalClipboardManager.current
    val parsed = remember(text) { NumberParser.parse(text) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speed Dialer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Paste your numbers",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Copy from Excel, WhatsApp or Notes and paste below — we'll pull out the numbers and any attached names.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Rahul: 98765 43210\nPriya 91234 56789\n+91 99887 76655") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            OutlinedButton(
                onClick = { clipboard.getText()?.text?.let { text = it } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Paste from clipboard")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (parsed.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else SuccessGreenLight
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (parsed.isNotEmpty()) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    }
                    Text(
                        if (parsed.isEmpty()) "No numbers found yet" else "${parsed.size} number${if (parsed.size == 1) "" else "s"} found",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (parsed.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = { onContinue(text, parsed) },
                enabled = parsed.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (parsed.isEmpty()) "Add at least one number to continue" else "Add these ${parsed.size}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
