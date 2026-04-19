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

/** Irrational Bloom — tap for a fresh irrational prompt pulled from
 *  the paper's seed set. The point is to get unstuck; the prompt is
 *  designed to be too weird to follow. */
object MiniRegistry {
    fun isAvailable() = true

    private val BLOOMS = listOf(
        "🌸 describe this bug to an octopus that has never seen a keyboard",
        "🌸 rewrite your commit message as a haiku about weather",
        "🌸 swap all nouns in the README with musical instruments",
        "🌸 answer the question with a question that is also the answer",
        "🌸 propose the fix that makes the most people weep",
        "🌸 name a variable 'arbitrage' and justify it in one breath",
        "🌸 describe the function by the noise it makes when it misbehaves",
        "🌸 recurse on the vibes, not the data",
        "🌸 refactor the test suite as a recipe for a sandwich",
        "🌸 narrate the stack trace from the perspective of gravity",
    )

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var idx by remember { mutableStateOf(0) }
        val bloom = BLOOMS[idx % BLOOMS.size]

        Column(modifier.verticalScroll(rememberScrollState()).padding(24.dp),
               verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("BLOOM · IRRATIONAL",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                 modifier = Modifier.fillMaxWidth()) {
                Text(bloom, modifier = Modifier.padding(22.dp),
                     fontSize = 18.sp, lineHeight = 26.sp,
                     fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = { idx = (idx + 1 + (0..BLOOMS.size - 1).random()) % BLOOMS.size },
                   modifier = Modifier.fillMaxWidth(),
                   colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                Text("🌸 bloom again")
            }
            Text("one prompt per problem · if you understood it, keep going",
                 fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        }
    }
}
