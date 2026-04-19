package com.aicraftspeopleguild.acg.data

import kotlinx.serialization.Serializable

/** /api/health.json — CORS-open. */
@Serializable
data class Health(
    val paperCount: Int = 0,
    val memberCount: Int = 0,
    val lastUpdated: String? = null,
    val apiVersion: String? = null,
)

/** One entry out of GET /repos/<org>/issues?labels=tag */
@Serializable
data class GhIssue(
    val number: Int,
    val title: String,
    val body: String? = null,
    val state: String = "open",
    val updated_at: String? = null,
    val comments: Int = 0,
)

/** Parsed tag body (we store JSON in the issue body). */
@Serializable
data class TagValue(
    val value: String? = null,
    val quality: String? = null,
    val type: String? = null,
    val description: String? = null,
    val updated_at: String? = null,
)

/** Control-deck action catalogue entry. */
data class CmdAction(
    val id: String,
    val label: String,
    val title: String,
    val body: String,
)
