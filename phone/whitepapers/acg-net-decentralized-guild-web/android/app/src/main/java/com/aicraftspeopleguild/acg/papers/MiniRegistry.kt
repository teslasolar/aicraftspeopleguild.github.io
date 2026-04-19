package com.aicraftspeopleguild.acg.papers

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Mesh Visualizer — add peer ids, watch them ring up as nodes with
 *  full-mesh edges. The paper's thesis about decentralized guild
 *  presence rendered as a pocket graph. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val ctx = LocalContext.current
        var peers by remember { mutableStateOf(load(ctx)) }
        var input by remember { mutableStateOf("") }

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MESH",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("${peers.size} node${if (peers.size == 1) "" else "s"} · ${peers.size * (peers.size - 1) / 2} edge(s)",
                 fontSize = 12.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Canvas(Modifier.fillMaxWidth().height(260.dp)) {
                val cx = size.width / 2; val cy = size.height / 2
                val r = (kotlin.math.min(size.width, size.height) / 2) - 30f
                val n = peers.size.coerceAtLeast(1)
                val pts = List(n) { i ->
                    val a = (i.toDouble() / n) * 2 * Math.PI - Math.PI / 2
                    Offset(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat())
                }
                for (i in pts.indices) for (j in i + 1 until pts.size) {
                    drawLine(primary.copy(alpha = 0.35f), pts[i], pts[j], strokeWidth = 1.2f)
                }
                for (p in pts) drawCircle(primary, 12f, p)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(input, { input = it.take(12) },
                                  label = { Text("peer id") }, singleLine = true,
                                  modifier = Modifier.weight(1f))
                Button(onClick = {
                    val t = input.trim()
                    if (t.isNotEmpty() && t !in peers) peers = (peers + t).takeLast(12)
                    save(ctx, peers); input = ""
                }, colors = ButtonDefaults.buttonColors(containerColor = primary)) { Text("+") }
            }
            if (peers.isNotEmpty()) {
                TextButton(onClick = { peers = emptyList(); save(ctx, peers) }) { Text("reset") }
            }
        }
    }

    private const val PREFS = "mini-mesh"
    private fun load(ctx: Context): List<String> =
        ctx.getSharedPreferences(PREFS, 0).getString("peers", "").orEmpty()
            .split(",").filter { it.isNotEmpty() }
    private fun save(ctx: Context, peers: List<String>) =
        ctx.getSharedPreferences(PREFS, 0).edit().putString("peers", peers.joinToString(",")).apply()
}
