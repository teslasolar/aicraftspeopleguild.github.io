package com.aicraftspeopleguild.acg.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local on-device mirror of the server-side `tag.db`. Stores three kinds
 * of rows, same shape as the Python ingestor:
 *
 *   tags          — one row per GH-Issues tag (issue_number unique on path)
 *   udts          — one row per UDT instance (udt_type + instance unique)
 *   script_events — one row per @tag-event trigger (id unique)
 *
 * Every write bumps `updated_at`. The SyncEngine + the live MQTT bus both
 * read/write through this DAO so UI state is driven by a single store.
 *
 * Zero dependencies — plain SQLiteOpenHelper, no Room, no kapt, no KSP.
 * The schema is forward-compatible: new tables only; add columns via
 * onUpgrade with ALTER TABLE when needed.
 */
class TagDb(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "acg-tags.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE tags (
              path          TEXT PRIMARY KEY,
              value         TEXT,
              quality       TEXT,
              type          TEXT,
              description   TEXT,
              issue_number  INTEGER,
              updated_at    TEXT,
              pulled_at     INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_tags_type ON tags(type)")

        db.execSQL("""
            CREATE TABLE udts (
              udt_type      TEXT NOT NULL,
              instance      TEXT NOT NULL,
              params_json   TEXT NOT NULL,
              source_path   TEXT,
              updated_at    INTEGER NOT NULL,
              PRIMARY KEY (udt_type, instance)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE script_events (
              id             TEXT PRIMARY KEY,
              script_file    TEXT NOT NULL,
              kind           TEXT,
              listens_tag    TEXT,
              listens_from   TEXT,
              listens_to     TEXT,
              action_tool_id TEXT,
              enabled        INTEGER DEFAULT 1,
              parsed_at      INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_script_events_tag ON script_events(listens_tag)")

        db.execSQL("""
            CREATE TABLE meta (
              key  TEXT PRIMARY KEY,
              val  TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, from: Int, to: Int) {
        // No upgrades yet — v1 is the initial schema.
    }

    // ─── Tags ────────────────────────────────────────────────────────

    fun upsertTag(path: String, value: TagValue, issueNumber: Int?) {
        writableDatabase.replace("tags", null, ContentValues().apply {
            put("path",         path)
            put("value",        value.value)
            put("quality",      value.quality)
            put("type",         value.type)
            put("description",  value.description)
            put("issue_number", issueNumber)
            put("updated_at",   value.updated_at)
            put("pulled_at",    System.currentTimeMillis())
        })
    }

    data class TagRow(
        val path: String, val value: String?, val quality: String?,
        val type: String?, val issueNumber: Int?, val updatedAt: String?,
    )

    fun listTags(prefix: String = ""): List<TagRow> {
        val sql = if (prefix.isBlank())
            "SELECT path, value, quality, type, issue_number, updated_at FROM tags ORDER BY path"
        else
            "SELECT path, value, quality, type, issue_number, updated_at FROM tags WHERE path LIKE ? ORDER BY path"
        val args = if (prefix.isBlank()) emptyArray() else arrayOf("$prefix%")
        val out = mutableListOf<TagRow>()
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                out += TagRow(
                    path        = c.getString(0),
                    value       = c.getString(1),
                    quality     = c.getString(2),
                    type        = c.getString(3),
                    issueNumber = if (c.isNull(4)) null else c.getInt(4),
                    updatedAt   = c.getString(5),
                )
            }
        }
        return out
    }

    fun readTag(path: String): TagRow? = listTags(path).firstOrNull { it.path == path }

    fun countTags(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM tags", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    // ─── UDTs ────────────────────────────────────────────────────────

    fun upsertUdt(udtType: String, instance: String, paramsJson: String, sourcePath: String? = null) {
        writableDatabase.replace("udts", null, ContentValues().apply {
            put("udt_type",    udtType)
            put("instance",    instance)
            put("params_json", paramsJson)
            put("source_path", sourcePath)
            put("updated_at",  System.currentTimeMillis())
        })
    }

    data class UdtRow(val udtType: String, val instance: String, val paramsJson: String)

    fun listUdts(udtType: String? = null): List<UdtRow> {
        val sql = if (udtType == null)
            "SELECT udt_type, instance, params_json FROM udts ORDER BY udt_type, instance"
        else
            "SELECT udt_type, instance, params_json FROM udts WHERE udt_type = ? ORDER BY instance"
        val args = if (udtType == null) emptyArray() else arrayOf(udtType)
        val out = mutableListOf<UdtRow>()
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) out += UdtRow(c.getString(0), c.getString(1), c.getString(2))
        }
        return out
    }

    // ─── Script events ───────────────────────────────────────────────

    data class ScriptEvent(
        val id: String, val scriptFile: String, val kind: String?,
        val listensTag: String?, val listensFrom: String?, val listensTo: String?,
        val actionToolId: String?, val enabled: Boolean,
    )

    fun upsertScriptEvent(s: ScriptEvent) {
        writableDatabase.replace("script_events", null, ContentValues().apply {
            put("id",             s.id)
            put("script_file",    s.scriptFile)
            put("kind",           s.kind)
            put("listens_tag",    s.listensTag)
            put("listens_from",   s.listensFrom)
            put("listens_to",     s.listensTo)
            put("action_tool_id", s.actionToolId)
            put("enabled",        if (s.enabled) 1 else 0)
            put("parsed_at",      System.currentTimeMillis())
        })
    }

    fun listScriptEvents(listensTag: String? = null): List<ScriptEvent> {
        val sql = if (listensTag == null)
            "SELECT id, script_file, kind, listens_tag, listens_from, listens_to, action_tool_id, enabled FROM script_events ORDER BY id"
        else
            "SELECT id, script_file, kind, listens_tag, listens_from, listens_to, action_tool_id, enabled FROM script_events WHERE listens_tag = ?"
        val args = if (listensTag == null) emptyArray() else arrayOf(listensTag)
        val out = mutableListOf<ScriptEvent>()
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                out += ScriptEvent(
                    id           = c.getString(0),
                    scriptFile   = c.getString(1),
                    kind         = c.getString(2),
                    listensTag   = c.getString(3),
                    listensFrom  = c.getString(4),
                    listensTo    = c.getString(5),
                    actionToolId = c.getString(6),
                    enabled      = c.getInt(7) == 1,
                )
            }
        }
        return out
    }

    // ─── meta (k/v) ──────────────────────────────────────────────────

    fun setMeta(key: String, value: String?) {
        writableDatabase.replace("meta", null, ContentValues().apply {
            put("key", key); put("val", value)
        })
    }
    fun getMeta(key: String): String? =
        readableDatabase.rawQuery("SELECT val FROM meta WHERE key = ?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
}
