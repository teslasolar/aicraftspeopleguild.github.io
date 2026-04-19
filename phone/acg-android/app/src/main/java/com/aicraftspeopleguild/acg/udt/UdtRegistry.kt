package com.aicraftspeopleguild.acg.udt

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads every *.template.json + instance-*.json + *-*.template.json
 * sitting in app/src/main/assets/udt/ at boot, groups them by udtType,
 * and exposes a single queryable catalog.
 *
 * Screens can now iterate `registry.instances("AndroidModule")` to
 * draw "what's running" without hardcoding a Kotlin list.
 */
class UdtRegistry(ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    data class Template(val udtType: String, val file: String, val description: String)
    data class Instance(
        val udtType: String, val instance: String, val file: String,
        val parameters: JsonObject,
    ) {
        fun str(key: String): String? = parameters[key]?.jsonPrimitive?.content
        fun strList(key: String): List<String> =
            (parameters[key] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
    }

    val templates: List<Template>
    val instances: List<Instance>

    init {
        val am = ctx.assets
        val files = am.list("udt")?.toList().orEmpty()
        val tpls = mutableListOf<Template>()
        val insts = mutableListOf<Instance>()

        for (f in files) {
            val text = am.open("udt/$f").bufferedReader().use { it.readText() }
            val doc  = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: continue
            val udtType = doc["udtType"]?.jsonPrimitive?.content ?: continue

            if (f.endsWith(".template.json")) {
                tpls += Template(
                    udtType     = udtType,
                    file        = "udt/$f",
                    description = doc["description"]?.jsonPrimitive?.content ?: "",
                )
            } else {
                // Instances carry {udtType, instance, parameters}
                val inst = doc["instance"]?.jsonPrimitive?.content ?: continue
                val params = doc["parameters"] as? JsonObject ?: continue
                insts += Instance(udtType, inst, "udt/$f", params)
            }
        }
        templates = tpls.sortedBy { it.udtType }
        instances = insts.sortedWith(compareBy({ it.udtType }, { it.instance }))
    }

    fun templates(udtType: String): List<Template> = templates.filter { it.udtType == udtType }
    fun instances(udtType: String): List<Instance> = instances.filter { it.udtType == udtType }
    fun typesPresent(): List<String> = instances.map { it.udtType }.distinct().sorted()
    fun countByType(): Map<String, Int> =
        instances.groupingBy { it.udtType }.eachCount().toSortedMap()
}
