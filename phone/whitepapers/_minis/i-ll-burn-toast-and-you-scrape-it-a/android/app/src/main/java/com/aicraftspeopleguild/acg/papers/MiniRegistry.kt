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

/** Burner vs Scraper — the paper's dichotomy, as a five-question
 *  typing quiz. Each answer nudges your score along the axis. At the
 *  end you get one of the four corners. */
object MiniRegistry {
    fun isAvailable() = true

    private data class Q(val prompt: String, val burnerAnswer: String, val scraperAnswer: String)

    private val QUIZ = listOf(
        Q("when the bug is already on fire you…",
          "ship the patch, retro later",
          "write the retro first, then ship"),
        Q("new framework drops this week…",
          "port a prototype tonight",
          "wait three releases and read the postmortems"),
        Q("the PR description you write is…",
          "one sentence and the test output",
          "four sections and the ADR"),
        Q("code review comments should…",
          "ship the change faster",
          "prevent the next one"),
        Q("the perfect Friday involves…",
          "one big-bet deploy",
          "nothing scheduled, on-call hat off"),
    )

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val answers = remember { mutableStateListOf<Int?>().also { repeat(QUIZ.size) { _ -> it.add(null) } } }
        val score: Int = answers.sumOf { a ->
            when (a) { 0 -> -1; 1 -> 1; else -> 0 }
        }
        val done = answers.none { it == null }
        val label = when {
            !done        -> "${answers.count { it != null }} / ${QUIZ.size}"
            score <= -3  -> "BURNER · torch in hand"
            score == -2 || score == -1 -> "BURNER-LEANING · shipper with sparks"
            score == 0   -> "AMBIDEXTROUS · rare"
            score in 1..2 -> "SCRAPER-LEANING · careful operator"
            else         -> "SCRAPER · the one cleaning up"
        }

        Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp),
               verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("BURNER / SCRAPER QUIZ",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primary)

            QUIZ.forEachIndexed { idx, q ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${idx + 1}. ${q.prompt}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Choice(q.burnerAnswer,  selected = answers[idx] == 0, primary) { answers[idx] = 0 }
                        Choice(q.scraperAnswer, selected = answers[idx] == 1, primary) { answers[idx] = 1 }
                    }
                }
            }
            if (done) {
                OutlinedButton(onClick = { for (i in answers.indices) answers[i] = null },
                               modifier = Modifier.fillMaxWidth()) { Text("reset") }
            }
        }
    }

    @Composable
    private fun RowScope.Choice(text: String, selected: Boolean, primary: Color, onTap: () -> Unit) {
        val c = if (selected) primary else Color(0xFF30363d)
        Card(colors = CardDefaults.cardColors(containerColor = if (selected) primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface),
             modifier = Modifier.weight(1f).padding(0.dp)) {
            TextButton(onClick = onTap, modifier = Modifier.fillMaxWidth()) {
                Text(text, fontSize = 12.sp, lineHeight = 16.sp,
                     color = if (selected) primary else MaterialTheme.colorScheme.onBackground,
                     fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
