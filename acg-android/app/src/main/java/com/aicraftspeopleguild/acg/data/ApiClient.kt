package com.aicraftspeopleguild.acg.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Thin OkHttp wrapper around the public L4 API. No auth required. */
class ApiClient(base: String = "https://aicraftspeopleguild.github.io") {
    private val baseUrl: String = base.trimEnd('/') + "/guild/Enterprise/L4"
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    @Throws(IOException::class)
    fun health(): Health = json.decodeFromString(getString("$baseUrl/api/health.json"))

    /** /runtime/tags.json — free-form; return the raw object so callers can drill in. */
    @Throws(IOException::class)
    fun runtimeTags(): JsonObject = json.parseToJsonElement(getString("$baseUrl/runtime/tags.json")).jsonObject

    data class EnterpriseSnapshot(
        val papers: Int, val members: Int, val programs: Int,
        val runs: Int, val tagEdges: Int, val authoredLinks: Int,
    )

    @Throws(IOException::class)
    fun enterprise(): EnterpriseSnapshot {
        val ent = runtimeTags()["enterprise"]?.jsonObject
            ?: return EnterpriseSnapshot(0, 0, 0, 0, 0, 0)
        // Fields come as either {value, quality, type} UDT wrappers or bare
        // primitives depending on the source. Handle both.
        fun pick(k: String): Int {
            val el: JsonElement = ent[k] ?: return 0
            val primitive: JsonPrimitive = when (el) {
                is JsonPrimitive -> el
                is JsonObject    -> (el["value"] as? JsonPrimitive) ?: return 0
                else             -> return 0
            }
            return primitive.content.toIntOrNull() ?: 0
        }
        return EnterpriseSnapshot(
            papers        = pick("paperCount"),
            members       = pick("memberCount"),
            programs      = pick("programCount"),
            runs          = pick("runCount"),
            tagEdges      = pick("tagEdges"),
            authoredLinks = pick("authoredLinks"),
        )
    }

    private fun getString(url: String): String {
        val req = Request.Builder().url(url).header("Accept", "application/json").build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} $url")
            return resp.body?.string().orEmpty()
        }
    }
}
