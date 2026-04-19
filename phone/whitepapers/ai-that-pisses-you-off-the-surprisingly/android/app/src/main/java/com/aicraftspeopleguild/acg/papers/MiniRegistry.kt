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

/** Pushback Dial — the paper argues an AI that pushes back builds
 *  deeper thinking. Slide the dial from agreeable to hostile and read
 *  the (canned) response it would give you at that level. */
object MiniRegistry {
    fun isAvailable() = true

    private val RUNGS = listOf(
        0    to "You are completely right, keep going exactly as planned.",
        15   to "Sounds reasonable. One quick question — have you tested the edge case where your assumption fails?",
        30   to "I'm not sure this holds. What's the specific evidence that your premise is true?",
        45   to "You're glossing over the hard part. Name the thing you're avoiding.",
        60   to "This argument collapses the moment someone asks for citations. Where are they?",
        75   to "You're confusing certainty with clarity. Prove you're not just attached.",
        90   to "This is the kind of take you'll regret in six months. Tell me why you won't.",
        100  to "No. Start over. You haven't earned this conclusion.",
    )

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var level by remember { mutableStateOf(30f) }
        val rung = RUNGS.minBy { kotlin.math.abs(it.first - level.toInt()) }

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("PUSHBACK DIAL",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("At level ${level.toInt()} the AI says:",
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                 modifier = Modifier.fillMaxWidth()) {
                Text(rung.second, modifier = Modifier.padding(18.dp),
                     fontSize = 15.sp, lineHeight = 22.sp)
            }
            Slider(value = level, onValueChange = { level = it },
                   valueRange = 0f..100f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("agreeable",  fontSize = 11.sp, color = Color(0xFF3fb950))
                Text("surgical",   fontSize = 11.sp, color = Color(0xFFe3b341))
                Text("scorched",   fontSize = 11.sp, color = Color(0xFFf85149))
            }
        }
    }
}
