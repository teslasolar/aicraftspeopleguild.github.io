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

/** Bayes for pocket use — the paper is about probabilistic reasoning
 *  without letting intuition run the show. Pick a prior (base rate),
 *  sensitivity (P(+|H)), and false-positive rate (P(+|~H)); the
 *  posterior P(H|+) tumbles out. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var prior by remember { mutableStateOf(0.05f) }
        var sens  by remember { mutableStateOf(0.90f) }
        var fpr   by remember { mutableStateOf(0.10f) }

        val numer = prior * sens
        val denom = numer + (1 - prior) * fpr
        val posterior = if (denom == 0f) 0f else numer / denom

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("BAYES POCKETBOOK",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("prior  P(H)    = ${"%.3f".format(prior)}",  fontFamily = FontFamily.Monospace)
            Slider(prior, { prior = it }, valueRange = 0.001f..0.999f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Text("sens   P(+|H)  = ${"%.3f".format(sens)}",   fontFamily = FontFamily.Monospace)
            Slider(sens, { sens = it }, valueRange = 0.001f..0.999f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Text("fpr    P(+|¬H) = ${"%.3f".format(fpr)}",    fontFamily = FontFamily.Monospace)
            Slider(fpr, { fpr = it }, valueRange = 0.001f..0.999f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            HorizontalDivider()
            Text("posterior  P(H|+)",
                 fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Text("${"%.4f".format(posterior)}  (${"%.1f".format(posterior * 100)}%)",
                 fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primary,
                 fontFamily = FontFamily.Monospace)
        }
    }
}
