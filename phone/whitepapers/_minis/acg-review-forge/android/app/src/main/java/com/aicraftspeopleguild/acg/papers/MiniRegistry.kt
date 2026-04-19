package {{ANDROID_PACKAGE}}

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Review Forge — the paper's set of canned review prompts turned
 *  into a tap-to-copy deck. No hand-wringing about which one to use:
 *  pick, paste, comment. */
object MiniRegistry {
    fun isAvailable() = true

    private val PROMPTS = listOf(
        "name the invariant this change protects. if you can't, the PR isn't ready",
        "what breaks first when this is wrong, and how does the on-call find out?",
        "trace the one path a hostile input could take through this function",
        "which test would have caught the previous version of this bug?",
        "if this gets reverted in six months, whose name is on the hotfix?",
        "explain the data flow in one sentence. if you can't, split the PR",
        "mark every line that would be unchanged by a completely different approach",
        "would you land this on Friday at 5pm? show your work",
        "is there a simpler thing that's 80% as good? who argues for it?",
        "circle the commit that would still ship if the others were reverted",
        "which reader needs to understand this in two years with no context?",
        "name the assumption that, if false, makes the entire change wrong",
    )

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val clip = LocalClipboardManager.current
        var flash by remember { mutableStateOf<String?>(null) }

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("REVIEW FORGE · ${PROMPTS.size}",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            flash?.let { Text(it, color = Color(0xFF3fb950), fontSize = 11.sp) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PROMPTS) { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                         modifier = Modifier.fillMaxWidth().clickable {
                             clip.setText(AnnotatedString(p)); flash = "copied: " + p.take(40) + "…"
                         }) {
                        Text(p, modifier = Modifier.padding(12.dp), fontSize = 13.sp,
                             lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}
