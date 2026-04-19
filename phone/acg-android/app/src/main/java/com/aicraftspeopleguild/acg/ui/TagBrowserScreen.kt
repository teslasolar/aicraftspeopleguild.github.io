package com.aicraftspeopleguild.acg.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicraftspeopleguild.acg.data.Mqtt
import com.aicraftspeopleguild.acg.data.SyncEngine
import com.aicraftspeopleguild.acg.data.TagDb
import kotlinx.coroutines.launch

@Composable
fun TagBrowserScreen(tagDb: TagDb, sync: SyncEngine, modifier: Modifier = Modifier) {
    var tags by remember { mutableStateOf(tagDb.listTags()) }
    var filter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Live refresh on any Mqtt tag.* publish
    DisposableEffect(Unit) {
        val unsub = Mqtt.subscribe("tag.#") { tags = tagDb.listTags() }
        onDispose { unsub() }
    }

    fun pullAll() = scope.launch {
        loading = true; status = null
        try {
            val r = sync.pullAllTags()
            tags = tagDb.listTags()
            status = "✓ ${r.tagsPulled} tags in ${r.durationMs}ms"
        } catch (e: Exception) { status = "✗ ${e.message}" }
        loading = false
    }

    val visible = remember(tags, filter) {
        if (filter.isBlank()) tags
        else tags.filter { it.path.contains(filter, ignoreCase = true) }
    }

    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            singleLine = true,
            label = { Text("filter path") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("${visible.size} of ${tags.size} (local)", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { pullAll() }, enabled = !loading) { Text("Sync") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        status?.let {
            val c = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Text(it, color = c, fontSize = 11.sp)
        }

        if (tags.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("No tags cached yet.",
                         color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { pullAll() }) { Text("Pull from GitHub") }
                }
            }
            return
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(visible, key = { it.path }) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { /* future: tag detail */ }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            row.path,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row {
                            row.value?.let {
                                Text("= ${it.take(40)}", fontFamily = FontFamily.Monospace,
                                     fontSize = 11.sp,
                                     color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                            }
                            row.type?.let {
                                Text(it, fontSize = 10.sp,
                                     color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        }
                    }
                    row.issueNumber?.let {
                        AssistChip(onClick = {}, label = { Text("#$it") })
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
