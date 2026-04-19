package com.aicraftspeopleguild.acg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicraftspeopleguild.acg.data.Mqtt
import com.aicraftspeopleguild.acg.data.SyncEngine
import com.aicraftspeopleguild.acg.data.TagDb
import com.aicraftspeopleguild.acg.data.TokenStore
import com.aicraftspeopleguild.acg.udt.UdtRegistry
import com.aicraftspeopleguild.acg.ui.organism.CatalogSection
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    tokens: TokenStore, tagDb: TagDb, sync: SyncEngine,
    registry: UdtRegistry,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf(tokens.getToken().orEmpty()) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GitHub token", style = MaterialTheme.typography.titleMedium)
        Text(
            "Required for tag_write + cmd_action. Stored in EncryptedSharedPreferences, excluded from cloud backup.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("GitHub PAT (repo scope)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                tokens.setToken(input.trim().takeIf { it.isNotEmpty() })
                status = if (tokens.hasToken()) "✓ saved" else "cleared"
            }) { Text("Save") }
            OutlinedButton(onClick = {
                input = ""; tokens.setToken(null); status = "cleared"
            }) { Text("Clear") }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Text("Local tag.db", style = MaterialTheme.typography.titleMedium)
        val scope = rememberCoroutineScope()
        var syncStatus by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        Text("tags cached: ${tagDb.countTags()}  ·  mqtt subs: ${Mqtt.subCount()}  ·  retained: ${Mqtt.retainedCount()}",
             fontSize = 11.sp,
             color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    busy = true; syncStatus = null
                    try {
                        val r = sync.pullAllTags()
                        syncStatus = "✓ ${r.tagsPulled} tags · ${r.errors} errors · ${r.durationMs}ms"
                    } catch (e: Exception) { syncStatus = "✗ ${e.message}" }
                    busy = false
                }
            }, enabled = !busy) { Text("Sync now") }
        }
        syncStatus?.let { Text(it, fontSize = 11.sp,
            color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        CatalogSection(registry)

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Text("About", style = MaterialTheme.typography.titleMedium)
        Text("ACG Android 0.3.0 · L0 sensor plant · local tag.db · Mqtt bus · UDT catalog.", fontSize = 12.sp)
        Text("Fork: teslasolar/aicraftspeopleguild.github.io  ·  watches origin too", fontSize = 12.sp,
             color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}
