package com.aicraftspeopleguild.acg.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls the GitHub-Issues tag DB into the local [TagDb], re-publishing
 * every fetched tag over [Mqtt] so live subscribers update in place.
 *
 * One-way-for-now: GH Issues -> device. Outbound writes still go direct
 * through [GhTagClient.writeTag]; after a successful write we refresh
 * the one affected path in the local cache.
 */
class SyncEngine(
    private val tagDb: TagDb,
    private val ghTag: GhTagClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class Result(val tagsPulled: Int, val errors: Int, val durationMs: Long)

    suspend fun pullAllTags(): Result = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var ok = 0; var errs = 0
        runCatching { ghTag.listTags() }.getOrDefault(emptyList()).forEach { issue ->
            val path = issue.title.removePrefix("tag:")
            runCatching { parseBody(issue.body) }.getOrNull()?.let { v ->
                tagDb.upsertTag(path, v, issue.number)
                Mqtt.publish("tag.$path", v.value, retained = true, from = "sync")
                ok++
            } ?: run { errs++ }
        }
        tagDb.setMeta("last_full_sync", System.currentTimeMillis().toString())
        Result(ok, errs, System.currentTimeMillis() - start)
    }

    /** Write to GH, then refresh just that one path locally. */
    suspend fun writeTagAndSync(
        path: String, value: String, type: String = "String",
        quality: String = "good", description: String = "written via ACG Android",
    ): String = withContext(Dispatchers.IO) {
        val outcome = ghTag.writeTag(path, value, type, quality, description)
        runCatching { ghTag.readTag(path) }.getOrNull()?.let { v ->
            tagDb.upsertTag(path, v, issueNumber = null)
            Mqtt.publish("tag.$path", v.value, retained = true, from = "write")
        }
        outcome
    }

    /** Refresh one tag from GH into the local DB (without writing). */
    suspend fun pullTag(path: String): TagValue? = withContext(Dispatchers.IO) {
        val v = runCatching { ghTag.readTag(path) }.getOrNull() ?: return@withContext null
        tagDb.upsertTag(path, v, issueNumber = null)
        Mqtt.publish("tag.$path", v.value, retained = true, from = "pull")
        v
    }

    private fun parseBody(body: String?): TagValue {
        if (body.isNullOrBlank()) return TagValue()
        var t = body.trim()
        if (t.startsWith("```")) {
            t = t.substringAfter("\n", t)
            if (t.endsWith("```")) t = t.substring(0, t.length - 3)
            if (t.trimStart().startsWith("json")) t = t.trimStart().removePrefix("json").trimStart()
        }
        return try { json.decodeFromString(TagValue.serializer(), t) } catch (_: Exception) { TagValue() }
    }
}
