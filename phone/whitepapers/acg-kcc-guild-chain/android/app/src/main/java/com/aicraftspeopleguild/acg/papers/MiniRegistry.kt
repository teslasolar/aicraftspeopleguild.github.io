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

/** Guild Coin ledger — the paper describes KCC, a private-fork
 *  accounting rail for guild operations. This is the thinnest
 *  possible running sketch: add entries with amount + memo, running
 *  balance updates. Persisted. No actual crypto. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val ctx = LocalContext.current
        var entries by remember { mutableStateOf(load(ctx)) }
        var amount by remember { mutableStateOf("") }
        var memo by remember { mutableStateOf("") }
        val balance = entries.sumOf { it.amount }

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("GUILD COIN LEDGER",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("balance", fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Text("$balance KCC", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '-' } },
                                  label = { Text("amount") }, singleLine = true,
                                  modifier = Modifier.weight(1f))
                OutlinedTextField(memo, { memo = it },
                                  label = { Text("memo") }, singleLine = true,
                                  modifier = Modifier.weight(2f))
            }
            Button(onClick = {
                val a = amount.toIntOrNull() ?: return@Button
                entries = (entries + Entry(System.currentTimeMillis(), a, memo.take(40))).takeLast(200)
                save(ctx, entries); amount = ""; memo = ""
            }, modifier = Modifier.fillMaxWidth(),
               colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                Text("post entry", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(entries.asReversed()) { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(e.memo.ifBlank { "—" },
                             fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                             modifier = Modifier.weight(1f))
                        Text((if (e.amount >= 0) "+" else "") + "${e.amount}",
                             fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                             fontWeight = FontWeight.Bold,
                             color = if (e.amount >= 0) Color(0xFF3fb950) else Color(0xFFf85149))
                    }
                }
            }
        }
    }

    private data class Entry(val ts: Long, val amount: Int, val memo: String)

    private const val PREFS = "mini-kcc"
    private fun load(ctx: Context): List<Entry> =
        ctx.getSharedPreferences(PREFS, 0).getString("ledger", "").orEmpty()
            .split("|").mapNotNull {
                val p = it.split(",", limit = 3)
                if (p.size == 3) p[0].toLongOrNull()?.let { ts ->
                    p[1].toIntOrNull()?.let { amt -> Entry(ts, amt, p[2]) }
                } else null
            }
    private fun save(ctx: Context, entries: List<Entry>) =
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putString("ledger", entries.joinToString("|") { "${it.ts},${it.amount},${it.memo.replace("|","/").replace(",",";")}" })
            .apply()
}
