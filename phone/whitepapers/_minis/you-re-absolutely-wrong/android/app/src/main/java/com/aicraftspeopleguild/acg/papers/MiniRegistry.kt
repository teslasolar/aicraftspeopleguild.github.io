package {{ANDROID_PACKAGE}}

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Certainty Reversal Log — the paper's thesis about trained-in
 *  uncertainty → calibration drift, turned into a personal ledger.
 *  Tap "I'm absolutely certain" to log a claim. Later, hit "was wrong"
 *  next to it to reverse. Reversal % is your real calibration. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val ctx = LocalContext.current
        var log by remember { mutableStateOf(load(ctx)) }
        val total = log.size
        val reversed = log.count { it.reversed }
        val pct = if (total == 0) 0 else reversed * 100 / total

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CERTAINTY REVERSAL LOG",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("$pct% of your absolute certainties got reversed ($reversed / $total)",
                 fontSize = 13.sp)
            Button(onClick = {
                log = (log + Claim(System.currentTimeMillis(), false)).takeLast(500)
                save(ctx, log)
            }, modifier = Modifier.fillMaxWidth(),
               colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                Text("I'm absolutely certain", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(log.asReversed()) { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("#${log.indexOf(c) + 1}  ·  ${stamp(c.ts)}",
                             fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                             color = if (c.reversed) Color(0xFFf85149) else Color(0xFFE6EDF3))
                        if (c.reversed) {
                            Text("reversed", color = Color(0xFFf85149),
                                 fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            TextButton(onClick = {
                                log = log.map { if (it === c) it.copy(reversed = true) else it }
                                save(ctx, log)
                            }) { Text("was wrong", color = primary) }
                        }
                    }
                }
            }
        }
    }

    private data class Claim(val ts: Long, val reversed: Boolean)

    private fun stamp(t: Long): String =
        java.text.SimpleDateFormat("MMM d · HH:mm", java.util.Locale.getDefault()).format(java.util.Date(t))

    private const val PREFS = "mini-certainty"
    private fun load(ctx: Context): List<Claim> =
        ctx.getSharedPreferences(PREFS, 0).getString("log", "").orEmpty()
            .split("|").mapNotNull {
                val p = it.split(",")
                if (p.size == 2) p[0].toLongOrNull()?.let { ts -> Claim(ts, p[1] == "1") } else null
            }
    private fun save(ctx: Context, log: List<Claim>) =
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putString("log", log.joinToString("|") { "${it.ts},${if (it.reversed) 1 else 0}" })
            .apply()
}
