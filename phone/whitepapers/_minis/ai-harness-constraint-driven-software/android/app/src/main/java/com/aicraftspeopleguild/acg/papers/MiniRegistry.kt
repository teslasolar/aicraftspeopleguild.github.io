package {{ANDROID_PACKAGE}}

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Constraint Ledger — the paper's premise is that agents produce
 *  better code when the harness names constraints up front. Add one
 *  per dev session, tick each when satisfied, revisit the misses. */
object MiniRegistry {
    fun isAvailable() = true

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        val ctx = LocalContext.current
        var items by remember { mutableStateOf(load(ctx)) }
        var input by remember { mutableStateOf("") }

        val met = items.count { it.met }
        val total = items.size

        Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CONSTRAINT LEDGER",
                 fontFamily = FontFamily.Monospace, color = primary,
                 fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("$met / $total met", fontFamily = FontFamily.Monospace,
                 fontSize = 16.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(input, { input = it.take(80) },
                              label = { Text("add constraint") }, singleLine = true,
                              modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val t = input.trim(); if (t.isEmpty()) return@Button
                items = (items + Item(System.currentTimeMillis(), t, false)).takeLast(80)
                save(ctx, items); input = ""
            }, modifier = Modifier.fillMaxWidth(),
               colors = ButtonDefaults.buttonColors(containerColor = primary)) { Text("+") }
            HorizontalDivider()
            LazyColumn(Modifier.heightIn(max = 340.dp)) {
                items(items) { it0 ->
                    val it = it0
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = it.met, onCheckedChange = { on ->
                            items = items.map { v -> if (v === it) v.copy(met = on) else v }
                            save(ctx, items)
                        }, colors = CheckboxDefaults.colors(checkedColor = primary))
                        Text(it.text, fontSize = 13.sp, modifier = Modifier.weight(1f),
                             textDecoration = if (it.met) TextDecoration.LineThrough else null,
                             color = if (it.met)
                                 MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                             else MaterialTheme.colorScheme.onBackground)
                        TextButton(onClick = {
                            items = items.filter { v -> v !== it }; save(ctx, items)
                        }) { Text("×", color = Color(0xFFf85149)) }
                    }
                }
            }
        }
    }

    private data class Item(val ts: Long, val text: String, val met: Boolean)

    private const val PREFS = "mini-constraint"
    private fun load(ctx: Context): List<Item> =
        ctx.getSharedPreferences(PREFS, 0).getString("items", "").orEmpty()
            .split("\n").mapNotNull {
                val p = it.split("|", limit = 3)
                if (p.size == 3) p[0].toLongOrNull()?.let { ts -> Item(ts, p[1], p[2] == "1") } else null
            }
    private fun save(ctx: Context, items: List<Item>) =
        ctx.getSharedPreferences(PREFS, 0).edit()
            .putString("items", items.joinToString("\n") { "${it.ts}|${it.text.replace("\n"," ").replace("|","/")}|${if (it.met) 1 else 0}" })
            .apply()
}
