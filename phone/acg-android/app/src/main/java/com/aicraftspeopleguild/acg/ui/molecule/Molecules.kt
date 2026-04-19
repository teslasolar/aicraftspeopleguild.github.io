package com.aicraftspeopleguild.acg.ui.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicraftspeopleguild.acg.udt.ActionRowParams
import com.aicraftspeopleguild.acg.udt.StatCardParams
import com.aicraftspeopleguild.acg.ui.atom.ActionButton
import com.aicraftspeopleguild.acg.ui.atom.StatRow

// ─── StatCard ─────────────────────────────────────────────────────

@Composable
fun StatCard(p: StatCardParams) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                p.title.uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            p.rows.forEach { StatRow(it) }
        }
    }
}

// ─── ActionRow ────────────────────────────────────────────────────

@Composable
fun ActionRow(p: ActionRowParams, onAction: (label: String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        p.buttons.forEach { b ->
            ActionButton(b) { onAction(b.label) }
        }
        if (p.busy) CircularProgressIndicator(Modifier.size(22.dp))
    }
}
