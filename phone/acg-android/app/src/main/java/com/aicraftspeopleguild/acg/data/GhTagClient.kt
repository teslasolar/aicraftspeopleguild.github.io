package com.aicraftspeopleguild.acg.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Mirror of bin/acg-mqtt.py / guild/Enterprise/L2/lib/gh_tag.py — the
 * GitHub Issues tag database, but accessed from the phone. Anonymous
 * reads (60 req/hr/IP) + PAT-authed writes. No search API use so we
 * dodge indexing lag — walk label:tag issues by title instead.
 */
class GhTagClient(
    private val repo: String = "teslasolar/aicraftspeopleguild.github.io",
    private val tokenProvider: () -> String? = { null },
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    /** List every open tag:* issue by title. */
    @Throws(IOException::class)
    fun listTags(): List<GhIssue> {
        val out = mutableListOf<GhIssue>()
        var page = 1
        while (page < 10) {
            val req = req("GET", "/repos/$repo/issues?labels=tag&state=open&per_page=100&page=$page")
            val body = http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
                r.body?.string().orEmpty()
            }
            val arr: JsonArray = try { json.parseToJsonElement(body).jsonArray } catch (_: Exception) { return out }
            val batch = arr.map { json.decodeFromJsonElement(GhIssue.serializer(), it) }
            out += batch.filter { it.title.startsWith("tag:") }
            if (batch.size < 100) return out
            page++
        }
        return out
    }

    @Throws(IOException::class)
    fun readTag(path: String): TagValue? {
        val issue = listTags().firstOrNull { it.title == "tag:$path" } ?: return null
        return parseBody(issue.body)
    }

    /** Upsert. Finds existing tag:<path> issue (open OR closed) and PATCHes body;
     *  otherwise POSTs a new one. Appends a comment with the new value for history. */
    @Throws(IOException::class)
    fun writeTag(path: String, value: String, type: String = "String",
                 quality: String = "good", description: String = "written via ACG Android"): String {
        val tok = tokenProvider() ?: throw IOException("no GitHub token configured")
        val now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ISO_INSTANT)
        val bodyJson = buildJsonObject {
            put("value", value); put("quality", quality); put("type", type)
            put("description", description); put("updated_at", now)
        }.toString()

        val existing = listTagsAllStates().firstOrNull { it.title == "tag:$path" }
        return if (existing == null) {
            val payload = buildJsonObject {
                put("title", "tag:$path")
                put("body", bodyJson)
                put("labels", buildJsonArray { add("tag") })
            }.toString()
            val r = http.newCall(req("POST", "/repos/$repo/issues", payload, tok)).execute()
            r.use { if (!it.isSuccessful) throw IOException("POST ${it.code}") }
            "created"
        } else {
            val patch = buildJsonObject { put("body", bodyJson) }.toString()
            http.newCall(req("PATCH", "/repos/$repo/issues/${existing.number}", patch, tok)).execute()
                .use { if (!it.isSuccessful) throw IOException("PATCH ${it.code}") }
            val commentPayload = buildJsonObject { put("body", bodyJson) }.toString()
            http.newCall(req("POST", "/repos/$repo/issues/${existing.number}/comments", commentPayload, tok)).execute()
                .use { /* best-effort comment, ignore body */ }
            "updated"
        }
    }

    private fun listTagsAllStates(): List<GhIssue> {
        val out = mutableListOf<GhIssue>()
        for (state in listOf("open", "closed")) {
            var page = 1
            var done = false
            while (!done && page < 10) {
                val req = req("GET", "/repos/$repo/issues?labels=tag&state=$state&per_page=100&page=$page")
                val body: String? = http.newCall(req).execute().use { r ->
                    if (r.isSuccessful) r.body?.string().orEmpty() else null
                }
                if (body == null) { done = true; continue }
                val arr = try { json.parseToJsonElement(body).jsonArray }
                          catch (_: Exception) { done = true; continue }
                val batch = arr.map { json.decodeFromJsonElement(GhIssue.serializer(), it) }
                out += batch.filter { it.title.startsWith("tag:") }
                if (batch.size < 100) done = true
                page++
            }
        }
        return out
    }

    private fun req(method: String, path: String, body: String? = null, token: String? = null): Request {
        val b = Request.Builder().url("https://api.github.com$path")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "acg-android/0.1")
        token?.let { b.header("Authorization", "Bearer $it") }
        val rb = body?.toRequestBody("application/json".toMediaType())
        return when (method) {
            "GET"   -> b.get().build()
            "POST"  -> b.post(rb ?: "".toRequestBody("application/json".toMediaType())).build()
            "PATCH" -> b.patch(rb ?: "".toRequestBody("application/json".toMediaType())).build()
            else    -> b.method(method, rb).build()
        }
    }

    private fun parseBody(body: String?): TagValue? {
        if (body.isNullOrBlank()) return null
        var t = body.trim()
        if (t.startsWith("```")) {
            t = t.substringAfter("\n", t)
            if (t.endsWith("```")) t = t.substring(0, t.length - 3)
            if (t.trimStart().startsWith("json")) t = t.trimStart().removePrefix("json").trimStart()
        }
        return try { json.decodeFromString(TagValue.serializer(), t) } catch (_: Exception) { null }
    }
}
