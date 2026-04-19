package {{ANDROID_PACKAGE}}

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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

/** Bias Flashcards — the paper's shield is literacy about your own
 *  cognitive failure modes. Tap a card to reveal the antidote. */
object MiniRegistry {
    fun isAvailable() = true

    private data class Bias(val name: String, val symptom: String, val antidote: String)

    private val DECK = listOf(
        Bias("Anchoring", "first number you heard is the one you argue around",
             "write the decision without the anchor visible, then compare"),
        Bias("Availability", "recent vivid events dominate the estimate",
             "count the base rate · ignore the story"),
        Bias("Confirmation", "evidence for > evidence against",
             "list three ways you'd be wrong before listing one you'd be right"),
        Bias("Sunk cost", "we already spent X so we must continue",
             "if starting today with no history, would you choose this path?"),
        Bias("Planning fallacy", "this'll take a week (it took three months)",
             "estimate the outside view · look at comparable past projects"),
        Bias("Dunning-Kruger", "can't see the ceiling from below it",
             "ask the person who makes you feel dumb · and listen"),
        Bias("Survivorship", "only successes are visible",
             "look for what's missing · the dead don't write memoirs"),
        Bias("Narrative fallacy", "a clean story feels true",
             "the world is noisier than any retrofit story · hold ambiguity"),
    )

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var open by remember { mutableStateOf(setOf<Int>()) }
        Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("BIAS FLASHCARDS · ${DECK.size}",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            DECK.forEachIndexed { i, b ->
                val isOpen = i in open
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                     modifier = Modifier.fillMaxWidth().clickable {
                         open = if (isOpen) open - i else open + i
                     }) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(b.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primary)
                            Spacer(Modifier.weight(1f))
                            Text(if (isOpen) "▾" else "▸", color = primary)
                        }
                        Text(b.symptom, fontSize = 12.sp,
                             color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        AnimatedVisibility(visible = isOpen) {
                            Text("antidote · ${b.antidote}",
                                 fontSize = 12.sp,
                                 color = MaterialTheme.colorScheme.onBackground,
                                 fontWeight = FontWeight.SemiBold,
                                 modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}
