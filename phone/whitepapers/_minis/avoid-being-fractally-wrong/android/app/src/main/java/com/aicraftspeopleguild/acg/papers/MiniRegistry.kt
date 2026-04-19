package {{ANDROID_PACKAGE}}

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fractal Wrongness — one slider controls depth. The deeper you go,
 *  the more self-similar the error becomes. Staring at it too long
 *  is the whole point of the paper. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var depth by remember { mutableStateOf(4f) }
        val d = depth.toInt()

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("FRACTAL WRONGNESS",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("every level wrong the same way",
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Canvas(Modifier.fillMaxWidth().height(320.dp)) {
                val w = size.width; val h = size.height
                val pad = 20f
                sierpinski(
                    Offset(w / 2, pad),
                    Offset(pad, h - pad),
                    Offset(w - pad, h - pad),
                    d, primary,
                ) { p1, p2, p3, c ->
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
                    }
                    drawPath(path, c, alpha = 0.85f)
                }
            }

            Text("depth: $d", fontFamily = FontFamily.Monospace)
            Slider(depth, { depth = it }, valueRange = 1f..7f, steps = 5,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Text("level 1 = only the outer mistake · level 7 = same mistake all the way down",
                 fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.sierpinski(
        a: Offset, b: Offset, c: Offset, depth: Int, color: Color,
        draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Offset, Offset, Offset, Color) -> Unit,
    ) {
        if (depth <= 0) { draw(a, b, c, color); return }
        val ab = midpoint(a, b); val bc = midpoint(b, c); val ca = midpoint(c, a)
        sierpinski(a, ab, ca, depth - 1, color, draw)
        sierpinski(ab, b, bc, depth - 1, color, draw)
        sierpinski(ca, bc, c, depth - 1, color, draw)
    }
    private fun midpoint(p: Offset, q: Offset) = Offset((p.x + q.x) / 2, (p.y + q.y) / 2)
}
