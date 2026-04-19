package {{ANDROID_PACKAGE}}

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Harm Calculator — severity × reach × (1 − reversibility). A
 *  one-equation sanity check before shipping anything that might
 *  land on someone else's day. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var severity by remember { mutableStateOf(3f) }      // 1..5
        var reach    by remember { mutableStateOf(3f) }      // 1..5
        var reversibility by remember { mutableStateOf(0.5f) } // 0..1

        val score = severity * reach * (1 - reversibility)
        val band = when {
            score >= 18 -> "CRITICAL · do not ship"
            score >= 10 -> "HIGH · mitigation required"
            score >= 4  -> "MODERATE · document + monitor"
            else        -> "LOW · proceed"
        }
        val color = when {
            score >= 18 -> Color(0xFFf85149)
            score >= 10 -> Color(0xFFe3b341)
            score >= 4  -> Color(0xFFf0883e)
            else        -> Color(0xFF3fb950)
        }

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("HARM EQUATION",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("severity (1–5): ${severity.toInt()}", fontFamily = FontFamily.Monospace)
            Slider(severity, { severity = it }, valueRange = 1f..5f, steps = 3,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Text("reach (1–5): ${reach.toInt()}", fontFamily = FontFamily.Monospace)
            Slider(reach, { reach = it }, valueRange = 1f..5f, steps = 3,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Text("reversibility: ${"%.2f".format(reversibility)}", fontFamily = FontFamily.Monospace)
            Slider(reversibility, { reversibility = it }, valueRange = 0f..1f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            HorizontalDivider()
            Text("score = sev × reach × (1 − rev)",
                 fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Text("%.1f".format(score),
                 fontSize = 40.sp, fontWeight = FontWeight.Bold, color = color,
                 fontFamily = FontFamily.Monospace)
            Text(band, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
