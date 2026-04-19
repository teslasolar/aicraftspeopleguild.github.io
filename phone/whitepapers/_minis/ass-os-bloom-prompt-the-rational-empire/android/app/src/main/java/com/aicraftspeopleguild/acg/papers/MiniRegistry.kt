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

/** Rational Bloom — for when the irrational one went too far and you
 *  need sober structure to ship. */
object MiniRegistry {
    fun isAvailable() = true

    private val BLOOMS = listOf(
        "🌸 list the three assumptions this plan depends on, in priority order",
        "🌸 write the failure case first · then the code · then the test",
        "🌸 name the invariant that must hold across every commit on this branch",
        "🌸 define success in one sentence before opening the editor",
        "🌸 rank the constraints · stop when you hit the one that can't move",
        "🌸 describe the interface using only the words the caller knows",
        "🌸 draw the data flow · highlight the one arrow you can't explain",
        "🌸 enumerate the exits before committing to the entrance",
        "🌸 say what this does NOT do, until someone asks",
        "🌸 convert your intuition into a checklist, then throw the checklist at an intern",
    )

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var idx by remember { mutableStateOf(0) }
        val bloom = BLOOMS[idx % BLOOMS.size]

        Column(modifier.verticalScroll(rememberScrollState()).padding(24.dp),
               verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("BLOOM · RATIONAL",
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
            Text("structure that keeps the empire running",
                 fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        }
    }
}
