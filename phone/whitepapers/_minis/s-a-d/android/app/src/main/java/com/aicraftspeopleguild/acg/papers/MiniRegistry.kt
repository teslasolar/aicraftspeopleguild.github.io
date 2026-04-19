package {{ANDROID_PACKAGE}}

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

/** Mood Log — the paper's S.A.D. riff, pocket-version. Tap today's
 *  score; the sparkline trails your last 30 days so the pattern you
 *  may or may not see is your own. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val ctx = LocalContext.current
        var entries by remember { mutableStateOf(load(ctx)) }
        val today = dayKey(System.currentTimeMillis())
        val todays = entries.lastOrNull { it.day == today }?.score
        val spark = (0..29).map { back ->
            entries.lastOrNull { it.day == today - back }?.score ?: -1
        }.reversed()

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("MOOD LOG · 30-DAY",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("today: ${todays ?: "—"}/10", fontFamily = FontFamily.Monospace, fontSize = 16.sp)

            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width; val h = size.height
                val dx = w / (spark.size - 1f)
                var prev: Offset? = null
                for ((i, s) in spark.withIndex()) {
                    if (s < 0) { prev = null; continue }
                    val x = i * dx
                    val y = h - (s / 10f) * h
                    val p = Offset(x, y)
                    drawCircle(primary, 4f, p)
                    prev?.let { drawLine(primary, it, p, strokeWidth = 2f) }
                    prev = p
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..10) {
                    OutlinedButton(onClick = {
                        entries = (entries + Entry(today, i)).takeLast(120)
                        save(ctx, entries)
                    }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                        Text("$i", fontSize = 11.sp,
                             fontFamily = FontFamily.Monospace,
                             color = if (i == todays) primary else MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            Text("1 = night · 10 = the good days",
                 fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        }
    }

    private data class Entry(val day: Long, val score: Int)

    private fun dayKey(ts: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return c.get(Calendar.YEAR) * 400L + c.get(Calendar.DAY_OF_YEAR)
    }

    private const val PREFS = "mini-sad"
    private fun load(ctx: Context): List<Entry> =
        ctx.getSharedPreferences(PREFS, 0).getString("entries", "").orEmpty()
            .split(",").mapNotNull {
                val p = it.split(":")
                if (p.size == 2) p[0].toLongOrNull()?.let { d -> p[1].toIntOrNull()?.let { s -> Entry(d, s) } } else null
            }
    private fun save(ctx: Context, entries: List<Entry>) =
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putString("entries", entries.joinToString(",") { "${it.day}:${it.score}" })
            .apply()
}
