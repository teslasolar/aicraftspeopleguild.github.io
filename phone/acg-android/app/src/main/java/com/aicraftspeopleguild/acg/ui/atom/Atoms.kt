package com.aicraftspeopleguild.acg.ui.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aicraftspeopleguild.acg.udt.ActionButtonParams
import com.aicraftspeopleguild.acg.udt.ButtonVariant
import com.aicraftspeopleguild.acg.udt.ChipColor
import com.aicraftspeopleguild.acg.udt.ChipParams
import com.aicraftspeopleguild.acg.udt.Severity
import com.aicraftspeopleguild.acg.udt.StatRowParams
import com.aicraftspeopleguild.acg.udt.StatusLineParams
import com.aicraftspeopleguild.acg.udt.ValueColor

// ─── StatRow ──────────────────────────────────────────────────────

@Composable
fun StatRow(p: StatRowParams) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(p.label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Text(p.value, fontWeight = FontWeight.SemiBold, color = p.valueColor.resolve())
    }
}

@Composable
private fun ValueColor.resolve(): Color = when (this) {
    ValueColor.TEXT    -> MaterialTheme.colorScheme.onBackground
    ValueColor.PRIMARY -> MaterialTheme.colorScheme.primary
    ValueColor.ERROR   -> MaterialTheme.colorScheme.error
    ValueColor.DIM     -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
}

// ─── ActionButton ─────────────────────────────────────────────────

@Composable
fun ActionButton(p: ActionButtonParams, onClick: () -> Unit) {
    when (p.variant) {
        ButtonVariant.PRIMARY -> Button(onClick = onClick, enabled = p.enabled) { Text(p.label) }
        ButtonVariant.SECONDARY -> OutlinedButton(onClick = onClick, enabled = p.enabled) { Text(p.label) }
        ButtonVariant.DANGER -> OutlinedButton(
            onClick = onClick,
            enabled = p.enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text(p.label) }
    }
}

// ─── StatusLine ───────────────────────────────────────────────────

@Composable
fun StatusLine(p: StatusLineParams) {
    Text(p.text, color = p.severity.resolve())
}

@Composable
private fun Severity.resolve(): Color = when (this) {
    Severity.OK   -> MaterialTheme.colorScheme.primary
    Severity.ERR  -> MaterialTheme.colorScheme.error
    Severity.INFO -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    Severity.WARN -> Color(0xFFE3B341)
}

// ─── Chip ─────────────────────────────────────────────────────────

@Composable
fun AcgChip(p: ChipParams, onClick: (() -> Unit)? = null) {
    AssistChip(
        onClick = { onClick?.invoke() },
        label = { Text(p.label) },
    )
}
