package com.aicraftspeopleguild.acg.papers

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * flywheel-tracker · the paper ("The Flywheel") says compounding
 * returns come from a small loop that builds on itself every cycle —
 * write, ship, review, teach, write. This mini is the minimum
 * implementation of that: one button to log a cycle, a streak counter
 * that grows while you keep coming back day after day, and the
 * running list of every cycle you've hit.
 *
 * Persistence: SharedPreferences "flywheel-mini" under key "cycles"
 * storing a comma-separated list of epoch-ms timestamps. Zero deps.
 */
object MiniRegistry {
    fun isAvailable(): Boolean = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val ctx = LocalContext.current
        var cycles by remember { mutableStateOf(loadCycles(ctx)) }

        val total = cycles.size
        val streak = computeStreak(cycles)
        val today  = cycles.count { isToday(it) }

        Column(
            modifier = modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("FLYWHEEL TRACKER",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp,
                 fontWeight = FontWeight.Bold)
            Text("one cycle per trip around the loop · the paper's bet is that this is the only loop that grows",
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("total",  "$total",  primary)
                Stat("streak", "$streak", primary)
                Stat("today",  "$today",  primary)
            }

            Button(
                onClick = {
                    cycles = (cycles + System.currentTimeMillis()).takeLast(1000)
                    saveCycles(ctx, cycles)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
            ) { Text("+ log a cycle", fontWeight = FontWeight.Bold) }

            OutlinedButton(
                onClick = {
                    cycles = emptyList(); saveCycles(ctx, cycles)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("reset") }

            HorizontalDivider()

            Text("RECENT",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 10.sp, letterSpacing = 2.sp,
                 fontWeight = FontWeight.Bold)
            val fmt = SimpleDateFormat("EEE · HH:mm", Locale.getDefault())
            if (cycles.isEmpty()) {
                Text("no cycles yet — the wheel starts here.",
                     fontSize = 12.sp,
                     color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(cycles.asReversed().take(40)) { ts ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${cycles.indexOf(ts) + 1}",
                                 fontFamily = FontFamily.Monospace,
                                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                 fontSize = 11.sp)
                            Text(fmt.format(Date(ts)),
                                 fontFamily = FontFamily.Monospace,
                                 fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Stat(label: String, value: String, primary: Color) {
    Column(
        modifier = Modifier.weight(1f)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = primary)
        Text(label.uppercase(),
             fontFamily = FontFamily.Monospace,
             fontSize = 9.sp, letterSpacing = 1.6.sp,
             color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

// ── persistence ──────────────────────────────────────────────────

private const val PREFS = "flywheel-mini"
private const val KEY   = "cycles"

private fun loadCycles(ctx: Context): List<Long> {
    val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
    return raw.split(",").mapNotNull { it.toLongOrNull() }.sorted()
}

private fun saveCycles(ctx: Context, cycles: List<Long>) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY, cycles.joinToString(","))
        .apply()
}

// ── computed stats ───────────────────────────────────────────────

private fun isToday(ts: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = ts }
    val b = Calendar.getInstance()
    return a.get(Calendar.YEAR)        == b.get(Calendar.YEAR) &&
           a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

/** Consecutive-day streak ending today (or yesterday if nothing today). */
private fun computeStreak(cycles: List<Long>): Int {
    if (cycles.isEmpty()) return 0
    val days = cycles.map { dayKey(it) }.toSortedSet()
    val today = dayKey(System.currentTimeMillis())
    // If nothing today, streak counts back from yesterday.
    var cursor = if (today in days) today else today - 1
    var streak = 0
    while (cursor in days) {
        streak++; cursor -= 1
    }
    return streak
}

private fun dayKey(ts: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return c.get(Calendar.YEAR) * 400L + c.get(Calendar.DAY_OF_YEAR)
}

