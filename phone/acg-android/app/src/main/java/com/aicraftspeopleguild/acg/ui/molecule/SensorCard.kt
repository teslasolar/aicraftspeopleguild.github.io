package com.aicraftspeopleguild.acg.ui.molecule

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicraftspeopleguild.acg.udt.SensorAccuracy
import com.aicraftspeopleguild.acg.udt.SensorReadingParams

@Composable
fun SensorCard(r: SensorReadingParams) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    r.kind.replace('_', ' '),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    r.accuracy.tag(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
            Text(
                r.name.take(44),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            val valStr = formatValues(r)
            Text(
                valStr,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text(
                "range ±${"%.3g".format(r.maxRange)} ${r.unit}  ·  res ${"%.3g".format(r.resolution)}  ·  min ${r.minDelayUs}µs",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
    }
}

private fun SensorAccuracy.tag(): String = when (this) {
    SensorAccuracy.HIGH       -> "● high"
    SensorAccuracy.MEDIUM     -> "● med"
    SensorAccuracy.LOW        -> "● low"
    SensorAccuracy.UNRELIABLE -> "● ??"
    SensorAccuracy.NONE       -> "○ idle"
}

private fun formatValues(r: SensorReadingParams): String {
    if (r.values.isEmpty()) return "—"
    val head = r.values.take(3).joinToString("  ") { "%+8.3f".format(it) }
    val tail = if (r.values.size > 3) "  +${r.values.size - 3}" else ""
    return "$head${if (r.unit.isNotEmpty()) "  ${r.unit}" else ""}$tail"
}
