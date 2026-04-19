package com.aicraftspeopleguild.acg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicraftspeopleguild.acg.data.GhTagClient
import com.aicraftspeopleguild.acg.data.SyncEngine
import com.aicraftspeopleguild.acg.data.TagValue
import com.aicraftspeopleguild.acg.data.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun HeartbeatScreen(ghTag: GhTagClient, tokens: TokenStore, sync: SyncEngine, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<TagValue?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var lastBumpResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() = scope.launch {
        busy = true; error = null
        try { current = withContext(Dispatchers.IO) { ghTag.readTag("demo.heartbeat") } }
        catch (e: Exception) { error = e.message }
        busy = false
    }

    fun bump() = scope.launch {
        if (!tokens.hasToken()) { error = "Set your PAT in Settings first"; return@launch }
        busy = true; error = null; lastBumpResult = null
        try {
            val epoch = System.currentTimeMillis() / 1000
            val outcome = sync.writeTagAndSync(
                path = "demo.heartbeat",
                value = epoch.toString(),
                type = "Counter",
                description = "bumped from ACG Android at ${Instant.now()}",
            )
            lastBumpResult = "✓ $outcome · $epoch"
            delay(500)
            current = withContext(Dispatchers.IO) { ghTag.readTag("demo.heartbeat") }
        } catch (e: Exception) { error = "✗ ${e.message}" }
        busy = false
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier.padding(24.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("💓", fontSize = 64.sp)
        Text("demo.heartbeat", fontFamily = FontFamily.Monospace,
             color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val v = current?.value
                val pretty = v?.toLongOrNull()?.let {
                    try {
                        DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)
                            .format(Instant.ofEpochSecond(it))
                    } catch (_: Exception) { v }
                } ?: "—"
                Text("value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                Text(v ?: "—", fontFamily = FontFamily.Monospace, fontSize = 20.sp)
                Text("utc", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                Text(pretty, fontFamily = FontFamily.Monospace)
                current?.quality?.let {
                    Text("quality: $it", fontSize = 11.sp,
                         color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { refresh() }, enabled = !busy) { Text("Refresh") }
            Button(onClick = { bump() }, enabled = !busy && tokens.hasToken()) {
                Text(if (tokens.hasToken()) "Bump heartbeat" else "Bump (no PAT)")
            }
        }
        if (busy) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        lastBumpResult?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
