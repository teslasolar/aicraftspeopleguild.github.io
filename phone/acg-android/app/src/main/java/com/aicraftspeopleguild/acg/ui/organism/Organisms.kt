package com.aicraftspeopleguild.acg.ui.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicraftspeopleguild.acg.udt.StatScreenParams
import com.aicraftspeopleguild.acg.ui.atom.StatusLine
import com.aicraftspeopleguild.acg.ui.molecule.ActionRow
import com.aicraftspeopleguild.acg.ui.molecule.StatCard

/**
 * Full scrollable screen composed from StatCards + one ActionRow +
 * one StatusLine. Home and Settings are each one instance.
 *
 * onAction is passed the button label so the caller dispatches on it.
 */
@Composable
fun StatScreen(
    p: StatScreenParams,
    modifier: Modifier = Modifier,
    onAction: (label: String) -> Unit = {},
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(p.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        p.subtitle?.let {
            Text(it, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        p.status?.let { StatusLine(it) }
        p.cards.forEach { StatCard(it) }
        p.actionRow?.let { ActionRow(it, onAction) }
    }
}
