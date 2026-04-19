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
import org.json.JSONObject

/** Konomi Validator — the Standard is a JSON-shaped UDT contract.
 *  Paste text, see whether it parses as a UDT instance
 *  (`{udtType, instance, parameters}`). */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var input by remember { mutableStateOf(SAMPLE) }
        val verdict = remember(input) { validate(input) }

        Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("KONOMI VALIDATOR",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("checks a string for {udtType, instance, parameters}",
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            OutlinedTextField(input, { input = it },
                              modifier = Modifier.fillMaxWidth().height(260.dp),
                              textStyle = androidx.compose.ui.text.TextStyle(
                                  fontFamily = FontFamily.Monospace, fontSize = 11.sp))
            HorizontalDivider()
            val color = if (verdict.ok) Color(0xFF3fb950) else Color(0xFFf85149)
            Text(if (verdict.ok) "✓ valid UDT instance" else "✗ invalid",
                 color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(verdict.msg, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
        }
    }

    private data class Verdict(val ok: Boolean, val msg: String)

    private fun validate(raw: String): Verdict {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Verdict(false, "empty input")
        return try {
            val obj = JSONObject(trimmed)
            val missing = listOf("udtType", "instance", "parameters").filter { !obj.has(it) }
            if (missing.isNotEmpty()) Verdict(false, "missing fields: " + missing.joinToString(", "))
            else Verdict(true, "udtType = ${obj.getString("udtType")}\n" +
                               "instance = ${obj.getString("instance")}\n" +
                               "parameters = ${obj.getJSONObject("parameters").length()} keys")
        } catch (e: Exception) { Verdict(false, "parse error: ${e.message}") }
    }

    private val SAMPLE = """
        {
          "udtType": "WhitepaperApp",
          "instance": "flywheel",
          "parameters": { "slug": "flywheel", "title": "The Flywheel" }
        }
    """.trimIndent()
}
