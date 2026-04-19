package com.aicraftspeopleguild.acg.papers

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Calibration Logger — the paper's trap is confident predictions
 *  that never get graded. Enter a claim with a probability, then come
 *  back later and mark it resolved. Average confidence of "happened"
 *  minus base rate shows your calibration bias. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val ctx = LocalContext.current
        var preds by remember { mutableStateOf(load(ctx)) }
        var txt by remember { mutableStateOf("") }
        var prob by remember { mutableStateOf(50f) }

        val resolved = preds.filter { it.outcome != null }
        val happenedAvgProb = resolved.filter { it.outcome == true }.map { it.prob }.ifEmpty { null }?.average()
        val missedAvgProb   = resolved.filter { it.outcome == false }.map { it.prob }.ifEmpty { null }?.average()

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CALIBRATION LOG",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("happened avg: ${happenedAvgProb?.let { "%.0f%%".format(it) } ?: "—"}   " +
                 "missed avg: ${missedAvgProb?.let { "%.0f%%".format(it) } ?: "—"}",
                 fontFamily = FontFamily.Monospace, fontSize = 12.sp)

            OutlinedTextField(txt, { txt = it.take(60) },
                              label = { Text("prediction") }, singleLine = true,
                              modifier = Modifier.fillMaxWidth())
            Text("confidence: ${prob.toInt()}%", fontFamily = FontFamily.Monospace)
            Slider(prob, { prob = it }, valueRange = 0f..100f,
                   colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary))
            Button(onClick = {
                if (txt.isBlank()) return@Button
                preds = (preds + Pred(System.currentTimeMillis(), txt, prob.toInt(), null)).takeLast(60)
                save(ctx, preds); txt = ""
            }, modifier = Modifier.fillMaxWidth(),
               colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                Text("log prediction")
            }
            HorizontalDivider()
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(preds.asReversed()) { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${p.prob}%", fontFamily = FontFamily.Monospace,
                             fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp),
                             color = primary)
                        Text(p.text, fontSize = 12.sp, modifier = Modifier.weight(1f),
                             color = when (p.outcome) {
                                 true  -> Color(0xFF3fb950)
                                 false -> Color(0xFFf85149)
                                 null  -> MaterialTheme.colorScheme.onBackground
                             })
                        if (p.outcome == null) {
                            TextButton(onClick = {
                                preds = preds.map { if (it === p) it.copy(outcome = true) else it }
                                save(ctx, preds)
                            }) { Text("✓", color = Color(0xFF3fb950)) }
                            TextButton(onClick = {
                                preds = preds.map { if (it === p) it.copy(outcome = false) else it }
                                save(ctx, preds)
                            }) { Text("✗", color = Color(0xFFf85149)) }
                        }
                    }
                }
            }
        }
    }

    private data class Pred(val ts: Long, val text: String, val prob: Int, val outcome: Boolean?)

    private const val PREFS = "mini-calib"
    private fun load(ctx: Context): List<Pred> =
        ctx.getSharedPreferences(PREFS, 0).getString("preds", "").orEmpty()
            .split("\n").mapNotNull {
                val p = it.split("|", limit = 4)
                if (p.size == 4) p[0].toLongOrNull()?.let { ts ->
                    p[2].toIntOrNull()?.let { pr ->
                        val out = when (p[3]) { "1" -> true; "0" -> false; else -> null }
                        Pred(ts, p[1], pr, out)
                    }
                } else null
            }
    private fun save(ctx: Context, preds: List<Pred>) =
        ctx.getSharedPreferences(PREFS, 0).edit().putString("preds",
            preds.joinToString("\n") {
                val o = when (it.outcome) { true -> "1"; false -> "0"; null -> "?" }
                "${it.ts}|${it.text.replace("\n"," ").replace("|","/")}|${it.prob}|$o"
            }).apply()
}
