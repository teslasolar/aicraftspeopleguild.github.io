package com.aicraftspeopleguild.acg.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.aicraftspeopleguild.acg.data.ApiClient
import com.aicraftspeopleguild.acg.data.Health
import com.aicraftspeopleguild.acg.data.SyncEngine
import com.aicraftspeopleguild.acg.data.TagDb
import com.aicraftspeopleguild.acg.udt.*
import com.aicraftspeopleguild.acg.ui.organism.StatScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Whole screen is declared as a StatScreenParams tree — no bespoke
 * layout code. Change a value, add a card, or flip a button variant
 * by editing data, not Compose.
 */
@Composable
fun HomeScreen(api: ApiClient, tagDb: TagDb, sync: SyncEngine, modifier: Modifier = Modifier) {
    var health by remember { mutableStateOf<Health?>(null) }
    var ent by remember { mutableStateOf<ApiClient.EnterpriseSnapshot?>(null) }
    var status by remember { mutableStateOf<StatusLineParams?>(null) }
    var loading by remember { mutableStateOf(false) }
    var localCount by remember { mutableStateOf(tagDb.countTags()) }
    var lastSync by remember { mutableStateOf(tagDb.getMeta("last_full_sync")) }
    val scope = rememberCoroutineScope()

    fun refresh() = scope.launch {
        loading = true; status = null
        try {
            withContext(Dispatchers.IO) {
                health = api.health(); ent = api.enterprise()
            }
        } catch (e: Exception) {
            status = StatusLineParams("✗ ${e.message}", Severity.ERR)
        }
        loading = false
    }

    fun syncTags() = scope.launch {
        loading = true; status = null
        try {
            val r = sync.pullAllTags()
            localCount = tagDb.countTags()
            lastSync = tagDb.getMeta("last_full_sync")
            status = StatusLineParams("✓ pulled ${r.tagsPulled} tags · ${r.errors} errors · ${r.durationMs}ms", Severity.OK)
        } catch (e: Exception) {
            status = StatusLineParams("✗ sync: ${e.message}", Severity.ERR)
        }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    StatScreen(
        modifier = modifier,
        p = StatScreenParams(
            title    = "⚒ AI Craftspeople Guild",
            subtitle = "ISA-95 live control-plane",
            status   = status,
            cards    = listOfNotNull(
                health?.let {
                    StatCardParams("catalog · /api/health.json", listOf(
                        StatRowParams("papers",  it.paperCount.toString()),
                        StatRowParams("members", it.memberCount.toString()),
                        StatRowParams("api",     it.apiVersion ?: "—"),
                        StatRowParams("updated", it.lastUpdated?.take(10) ?: "—"),
                    ))
                },
                ent?.let {
                    StatCardParams("enterprise · /runtime/tags.json", listOf(
                        StatRowParams("papers",        it.papers.toString()),
                        StatRowParams("members",       it.members.toString()),
                        StatRowParams("programs",      it.programs.toString()),
                        StatRowParams("runs",          it.runs.toString()),
                        StatRowParams("tag edges",     it.tagEdges.toString()),
                        StatRowParams("authored lnks", it.authoredLinks.toString()),
                    ))
                },
                StatCardParams("local cache", listOf(
                    StatRowParams("tags",      localCount.toString()),
                    StatRowParams("last sync", lastSync?.toLongOrNull()?.let { formatAgo(it) } ?: "never"),
                )),
            ),
            actionRow = ActionRowParams(
                busy = loading,
                buttons = listOf(
                    ActionButtonParams("Refresh",   ButtonVariant.PRIMARY,   enabled = !loading),
                    ActionButtonParams("Sync tags", ButtonVariant.SECONDARY, enabled = !loading),
                ),
            ),
        ),
        onAction = { label ->
            when (label) {
                "Refresh"   -> refresh()
                "Sync tags" -> syncTags()
            }
        },
    )
}

private fun formatAgo(epochMs: Long): String {
    val secs = (System.currentTimeMillis() - epochMs) / 1000
    return when {
        secs < 60    -> "${secs}s ago"
        secs < 3600  -> "${secs / 60}m ago"
        secs < 86400 -> "${secs / 3600}h ago"
        else         -> "${secs / 86400}d ago"
    }
}
