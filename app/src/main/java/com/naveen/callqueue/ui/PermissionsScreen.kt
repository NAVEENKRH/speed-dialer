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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PermPhoneMsg
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naveen.callqueue.ui.theme.BrandBlue
import com.naveen.callqueue.ui.theme.BrandBlueLight
import com.naveen.callqueue.ui.theme.SuccessGreen
import com.naveen.callqueue.ui.theme.SuccessGreenLight

data class PermissionExplainer(val icon: ImageVector, val title: String, val reason: String)

private val EXPLAINERS = listOf(
    PermissionExplainer(Icons.Filled.Call, "Phone", "So the app can place calls on your behalf without you tapping the dialer's call button each time."),
    PermissionExplainer(Icons.Filled.PermPhoneMsg, "Phone state", "So the app can tell when a call ends and automatically move to the next number."),
    PermissionExplainer(Icons.Filled.History, "Call log", "So the app can check whether a call actually connected, to auto-redial a genuine no-answer."),
    PermissionExplainer(Icons.Filled.Notifications, "Notifications", "So Android lets the app keep running while it's dialing through your list.")
)

@Composable
fun PermissionsScreen(
    permissionsGranted: Boolean,
    onGrantClick: () -> Unit,
    batteryExempt: Boolean,
    onRequestBatteryExemption: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).background(BrandBlueLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp))
            }
            Text("A couple of things first", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Speed Dialer only calls numbers you paste in yourself, and never sends or stores anything outside this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            EXPLAINERS.forEach { item ->
                ExplainerCard(item.icon, item.title, item.reason)
            }

            if (!permissionsGranted) {
                Button(
                    onClick = onGrantClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Grant permissions", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                GrantedRow("Permissions granted")
            }

            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            ExplainerCard(
                Icons.Filled.BatteryChargingFull,
                "Battery",
                "Samsung phones can silently stop background apps the moment a call starts, which would cut the auto-dial session off after the first call. This keeps Speed Dialer running for the whole session."
            )

            if (!batteryExempt) {
                Button(
                    onClick = onRequestBatteryExemption,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Allow to run in background", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                GrantedRow("Background running allowed")
            }
        }
    }
}

@Composable
private fun ExplainerCard(icon: ImageVector, title: String, reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(BrandBlueLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GrantedRow(label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessGreenLight)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
            Text(label, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}
