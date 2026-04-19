package com.aicraftspeopleguild.acg.papers

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

/** Occam Filter — paste something verbose. The filter strips filler
 *  adverbs, doubled spacing, and weasel phrases so what's left is the
 *  claim you were actually making. */
object MiniRegistry {
    fun isAvailable() = true

    private val FILLER_WORDS = setOf(
        "very", "really", "actually", "basically", "literally", "just", "simply",
        "quite", "rather", "somewhat", "maybe", "perhaps", "arguably", "clearly",
        "obviously", "definitely", "certainly", "naturally", "essentially",
    )
    private val WEASELS = listOf(
        Regex("""\bin order to\b""",    RegexOption.IGNORE_CASE) to "to",
        Regex("""\bdue to the fact that\b""", RegexOption.IGNORE_CASE) to "because",
        Regex("""\bat this point in time\b""", RegexOption.IGNORE_CASE) to "now",
        Regex("""\bit goes without saying that\b""", RegexOption.IGNORE_CASE) to "",
        Regex("""\bneedless to say\b""", RegexOption.IGNORE_CASE) to "",
        Regex("""\bfor all intents and purposes\b""", RegexOption.IGNORE_CASE) to "",
    )

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var input by remember { mutableStateOf(SAMPLE) }
        val (out, saved) = remember(input) { occam(input) }

        Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("OCCAM FILTER",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("saved $saved char(s)", fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            OutlinedTextField(input, { input = it }, label = { Text("verbose") },
                              modifier = Modifier.fillMaxWidth().height(180.dp),
                              textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp))
            HorizontalDivider()
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                 modifier = Modifier.fillMaxWidth()) {
                Text(out.ifBlank { "(nothing left.)" },
                     modifier = Modifier.padding(16.dp),
                     fontSize = 15.sp, lineHeight = 21.sp,
                     color = if (out.isBlank()) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                             else MaterialTheme.colorScheme.onBackground,
                     fontWeight = FontWeight.SemiBold)
            }
        }
    }

    private fun occam(raw: String): Pair<String, Int> {
        var s = raw
        for ((re, r) in WEASELS) s = s.replace(re, r)
        s = s.split(Regex("""\s+"""))
            .filter { it.lowercase().trimEnd('.', ',', '!', '?') !in FILLER_WORDS }
            .joinToString(" ")
        s = s.replace(Regex("""\s+([.,!?])"""), "$1").trim()
        return s to (raw.length - s.length)
    }

    private val SAMPLE = "It goes without saying that we should, in order to make a decision, very clearly and obviously actually consider all the options that are arguably somewhat relevant at this point in time."
}
