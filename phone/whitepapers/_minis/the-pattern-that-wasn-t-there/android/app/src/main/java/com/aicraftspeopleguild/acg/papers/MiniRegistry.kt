package {{ANDROID_PACKAGE}}

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/** The Pattern That Wasn't There — generate a field of random dots,
 *  let the user tap the ones that "form a shape". The dots never
 *  form anything; whatever they "found" is pareidolia. Round counter
 *  tracks how many times they fell for it. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var seed by remember { mutableStateOf(System.currentTimeMillis()) }
        var marked by remember { mutableStateOf(setOf<Int>()) }
        var rounds by remember { mutableStateOf(0) }
        var totalMarks by remember { mutableStateOf(0) }
        val rng = remember(seed) { Random(seed) }
        val pts = remember(seed) {
            List(60) { Offset(rng.nextFloat(), rng.nextFloat()) }
        }

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("THE PATTERN THAT WASN'T THERE",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("tap dots that form a shape · they don't",
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Canvas(Modifier.fillMaxWidth().height(320.dp).pointerInput(seed) {
                detectTapGestures { pos ->
                    val (w, h) = size.width.toFloat() to size.height.toFloat()
                    val hit = pts.withIndex().minBy {
                        val c = it.value; val d = Offset(c.x * w - pos.x, c.y * h - pos.y)
                        d.x * d.x + d.y * d.y
                    }
                    marked = if (hit.index in marked) marked - hit.index else marked + hit.index
                }
            }) {
                val w = size.width; val h = size.height
                for ((i, p) in pts.withIndex()) {
                    val isMarked = i in marked
                    drawCircle(
                        color = if (isMarked) primary else Color(0xFF8B949E).copy(alpha = 0.6f),
                        radius = if (isMarked) 14f else 6f,
                        center = Offset(p.x * w, p.y * h),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("rounds played: $rounds", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("patterns found: $totalMarks",
                     fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                     color = if (totalMarks > 0) Color(0xFFf85149) else MaterialTheme.colorScheme.onBackground)
            }
            Button(onClick = {
                totalMarks += marked.size
                rounds     += 1
                marked = emptySet()
                seed = System.currentTimeMillis() + rounds
            }, modifier = Modifier.fillMaxWidth(),
               colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                Text("new grid")
            }
            Text("if that number isn't 0, that's you adding the pattern",
                 fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        }
    }
}
