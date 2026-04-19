package {{ANDROID_PACKAGE}}

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Integrity Inspector — the paper pushes past "correctness" to
 *  measurable integrity. Paste some code; the inspector scores it on
 *  cheap heuristics that correlate with honest-ish code. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var code by remember { mutableStateOf(SAMPLE) }
        val m = remember(code) { measure(code) }

        Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("INTEGRITY INSPECTOR",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(code, { code = it },
                              modifier = Modifier.fillMaxWidth().height(220.dp),
                              textStyle = androidx.compose.ui.text.TextStyle(
                                  fontFamily = FontFamily.Monospace, fontSize = 11.sp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("lines", "${m.lines}", primary, Modifier.weight(1f))
                Stat("long",  "${m.longLines}", primary, Modifier.weight(1f))
                Stat("TODO",  "${m.todos}", primary, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("placeholders", "${m.placeholders}", primary, Modifier.weight(1f))
                Stat("comments %",   "${m.commentPct}",   primary, Modifier.weight(1f))
                Stat("score",        "${m.score}/100",    primary, Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun Stat(label: String, value: String, primary: Color, mod: Modifier) {
        Column(mod.padding(10.dp),
               horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primary,
                 fontFamily = FontFamily.Monospace)
            Text(label.uppercase(),
                 fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.6.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }

    private data class Metrics(
        val lines: Int, val longLines: Int, val todos: Int,
        val placeholders: Int, val commentPct: Int, val score: Int,
    )

    private fun measure(s: String): Metrics {
        val lines = s.lines()
        val n     = lines.size.coerceAtLeast(1)
        val long  = lines.count { it.length > 100 }
        val todo  = lines.count { it.contains("TODO") || it.contains("FIXME") || it.contains("XXX") }
        val ph    = lines.count { it.contains("...") || it.contains("pass") || it.contains("NotImplementedError") }
        val cmt   = lines.count { it.trim().startsWith("//") || it.trim().startsWith("#") || it.trim().startsWith("*") }
        val cpct  = cmt * 100 / n
        val score = (100 - long * 2 - todo * 5 - ph * 6 + (if (cpct in 5..20) 5 else 0))
            .coerceIn(0, 100)
        return Metrics(n, long, todo, ph, cpct, score)
    }

    private val SAMPLE = """
        fun compute(x: Int): Int {
            // main-path: halve until we hit 1
            var n = x
            while (n > 1) { n = n / 2 }
            return n
        }
    """.trimIndent()
}
