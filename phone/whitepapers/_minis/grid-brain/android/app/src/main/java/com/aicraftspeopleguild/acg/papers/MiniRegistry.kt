package {{ANDROID_PACKAGE}}

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Grid Brain — the paper's neurons-on-a-grid idea as a Game-of-Life
 *  toy. Tap to set cells; start the clock and the grid evolves per
 *  classic B3/S23 rules. Reset to seed something else. */
object MiniRegistry {
    fun isAvailable() = true

    private const val N = 18  // grid side

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var cells by remember { mutableStateOf(Array(N) { BooleanArray(N) }) }
        var running by remember { mutableStateOf(false) }
        var gen by remember { mutableStateOf(0) }

        LaunchedEffect(running) {
            while (running) {
                delay(220)
                cells = step(cells); gen += 1
                if (cells.sumOf { row -> row.count { it } } == 0) running = false
            }
        }

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("GRID BRAIN",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("gen $gen · tap to seed · play to tick · B3/S23",
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Canvas(Modifier.fillMaxWidth().height(360.dp).pointerInput(Unit) {
                detectTapGestures { pos ->
                    val cell = minOf(size.width, size.height) / N.toFloat()
                    val col = (pos.x / cell).toInt().coerceIn(0, N - 1)
                    val row = (pos.y / cell).toInt().coerceIn(0, N - 1)
                    val copy = cells.map { it.copyOf() }.toTypedArray()
                    copy[row][col] = !copy[row][col]; cells = copy
                }
            }) {
                val cell = minOf(size.width, size.height) / N.toFloat()
                for (r in 0 until N) for (c in 0 until N) {
                    val x = c * cell; val y = r * cell
                    drawRect(color = if (cells[r][c]) primary
                                     else primary.copy(alpha = 0.08f),
                             topLeft = Offset(x + 1f, y + 1f),
                             size = Size(cell - 2f, cell - 2f))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { running = !running },
                       modifier = Modifier.weight(1f),
                       colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                    Text(if (running) "pause" else "play")
                }
                OutlinedButton(onClick = {
                    cells = Array(N) { BooleanArray(N) }; gen = 0; running = false
                }, modifier = Modifier.weight(1f)) { Text("reset") }
            }
        }
    }

    private fun step(g: Array<BooleanArray>): Array<BooleanArray> {
        val n = g.size
        val out = Array(n) { BooleanArray(n) }
        for (r in 0 until n) for (c in 0 until n) {
            var k = 0
            for (dr in -1..1) for (dc in -1..1) if (dr != 0 || dc != 0) {
                val rr = (r + dr + n) % n; val cc = (c + dc + n) % n
                if (g[rr][cc]) k++
            }
            out[r][c] = if (g[r][c]) k == 2 || k == 3 else k == 3
        }
        return out
    }
}
