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
import kotlinx.coroutines.delay

/** Toast Carbonization Clock — start the timer, walk away. Past 60s
 *  the app turns red and names the failure mode (per the paper,
 *  distinct from voluntary tech debt: nobody chose this, it just
 *  stayed on the burner). Reset when you notice. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var started by remember { mutableStateOf<Long?>(null) }
        var now by remember { mutableStateOf(System.currentTimeMillis()) }

        LaunchedEffect(started) {
            while (started != null) {
                now = System.currentTimeMillis(); delay(250)
            }
        }

        val elapsed = started?.let { (now - it) / 1000 } ?: 0L
        val stage = when {
            started == null   -> Stage("cold",         "bread is fine. no heat yet.", Color(0xFF8B949E))
            elapsed < 30      -> Stage("toasting",     "normal.", Color(0xFF3fb950))
            elapsed < 60      -> Stage("brown",        "the intended state. take it out.", Color(0xFFe3b341))
            elapsed < 120     -> Stage("carbonizing",  "volatiles leaving · structural change beginning.", Color(0xFFf0883e))
            elapsed < 240     -> Stage("carbonized",   "matrix failure · no longer food", Color(0xFFf85149))
            else              -> Stage("ash",          "technical debt acquired entirely passively", Color(0xFFf85149))
        }

        Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("TOAST CARBONIZATION",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("${elapsed}s",
                 fontSize = 72.sp, fontWeight = FontWeight.Bold, color = stage.color,
                 fontFamily = FontFamily.Monospace)
            Text(stage.name.uppercase(), fontSize = 20.sp,
                 color = stage.color, fontWeight = FontWeight.Bold)
            Text(stage.blurb,
                 fontSize = 13.sp, lineHeight = 18.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { started = System.currentTimeMillis() },
                       modifier = Modifier.weight(1f),
                       colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                    Text(if (started == null) "start" else "restart")
                }
                OutlinedButton(onClick = { started = null },
                               modifier = Modifier.weight(1f)) { Text("take it out") }
            }
        }
    }

    private data class Stage(val name: String, val blurb: String, val color: Color)
}
