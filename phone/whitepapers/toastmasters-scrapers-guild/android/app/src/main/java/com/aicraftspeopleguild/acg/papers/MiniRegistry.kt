package com.aicraftspeopleguild.acg.papers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** URL Auditor — the paper's "scraper" impulse, made safe. Paste a
 *  URL; the inspector pulls it apart and names the things that could
 *  go wrong before you ever fetch it. No network calls. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var url by remember { mutableStateOf("https://example.com/articles/42?utm_source=x") }
        val audit = remember(url) { audit(url) }

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("URL AUDITOR",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(url, { url = it }, label = { Text("url") },
                              singleLine = true, modifier = Modifier.fillMaxWidth(),
                              textStyle = androidx.compose.ui.text.TextStyle(
                                  fontFamily = FontFamily.Monospace, fontSize = 12.sp))
            HorizontalDivider()
            audit.forEach { line ->
                Row {
                    Text(line.tag, fontFamily = FontFamily.Monospace,
                         color = line.color, fontWeight = FontWeight.Bold,
                         modifier = Modifier.width(60.dp), fontSize = 11.sp)
                    Text(line.msg, fontSize = 12.sp,
                         color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
                }
            }
        }
    }

    private data class Line(val tag: String, val msg: String, val color: Color)

    private fun audit(raw: String): List<Line> {
        val out = mutableListOf<Line>()
        val s = raw.trim()
        if (s.isEmpty()) return listOf(Line("—", "(empty)", Color(0xFF8B949E)))
        val ok   = Color(0xFF3fb950); val warn = Color(0xFFe3b341); val bad = Color(0xFFf85149)

        if (!s.startsWith("https://")) out += Line("WARN", "not https — downstream UA may refuse", warn)
        else                           out += Line("OK",   "https", ok)

        val m = Regex("""^https?://([^/]+)(/[^?#]*)?(\?[^#]*)?(#.*)?$""").matchEntire(s)
        if (m == null) { out += Line("FAIL", "malformed URL", bad); return out }
        val (_, host, path, query) = m.destructured.let { Tuple4(m.value, it.component1(), it.component2(), it.component3()) }

        out += Line("HOST",  host, ok)
        if (path.isNotEmpty()) out += Line("PATH", path, ok)
        if (query.isNotEmpty()) {
            val params = query.removePrefix("?").split("&").size
            out += Line("QS",   "$params param(s)", ok)
            if (query.contains("utm_"))
                out += Line("WARN", "utm_ tracking parameters present", warn)
            if (query.length > 500)
                out += Line("WARN", "long query string — may hit server limits", warn)
        }
        if ("localhost" in host || host.startsWith("10.") || host.startsWith("192.168."))
            out += Line("WARN", "private address — won't resolve off-LAN", warn)
        return out
    }

    private data class Tuple4(val a: String, val b: String, val c: String, val d: String)
}
