"""
tag_state — runtime tag values (Value/Quality/Timestamp) in a separate
SQLite so tag.db (metadata, regenerable) stays clean.

File:  guild/Enterprise/L2/state/tag-values.db
Table: tag_values (tag PK, value, quality, updated_at)
       tag_history (tag, value, quality, at) — append-only audit

read(tag)         -> {value, quality, updated_at} or None
write(tag, value, quality='good') -> dict
list_prefix(p)    -> {tag: {...}, ...}
"""
import json, sqlite3
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
DB   = REPO / "guild" / "Enterprise" / "L2" / "state" / "tag-values.db"

_SCHEMA = """
CREATE TABLE IF NOT EXISTS tag_values (
    tag        TEXT PRIMARY KEY,
    value      TEXT,
    quality    TEXT DEFAULT 'good',
    updated_at TEXT
);
CREATE TABLE IF NOT EXISTS tag_history (
    tag        TEXT NOT NULL,
    value      TEXT,
    quality    TEXT,
    at         TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tag_history_tag ON tag_history(tag, at);
"""


def _now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _conn() -> sqlite3.Connection:
    DB.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(str(DB))
    con.executescript(_SCHEMA)
    return con


def _s(v):
    if v is None: return None
    return v if isinstance(v, str) else json.dumps(v, ensure_ascii=False, sort_keys=True)


def read(tag: str) -> dict:
    con = _conn()
    try:
        row = con.execute(
            "SELECT value, quality, updated_at FROM tag_values WHERE tag=?", (tag,)
        ).fetchone()
        if not row:
            return {"ok": True, "tag": tag, "value": None, "quality": "uncertain", "updated_at": None}
        return {"ok": True, "tag": tag, "value": row[0], "quality": row[1], "updated_at": row[2]}
    finally:
        con.close()


def write(tag: str, value=None, quality: str = "good") -> dict:
    """Write a tag value. Returns the previous value so the caller (state machine)
    can emit an on_transition event if it changed."""
    now = _now()
    v = _s(value)
    con = _conn()
    try:
        prev = con.execute(
            "SELECT value, quality FROM tag_values WHERE tag=?", (tag,)
        ).fetchone()
        prev_value   = prev[0] if prev else None
        prev_quality = prev[1] if prev else None
        con.execute(
            "INSERT INTO tag_values(tag, value, quality, updated_at) VALUES (?,?,?,?) "
            "ON CONFLICT(tag) DO UPDATE SET value=excluded.value, quality=excluded.quality, updated_at=excluded.updated_at",
            (tag, v, quality, now),
        )
        con.execute(
            "INSERT INTO tag_history(tag, value, quality, at) VALUES (?,?,?,?)",
            (tag, v, quality, now),
        )
        con.commit()
        return {
            "ok": True, "tag": tag,
            "value": v, "quality": quality, "updated_at": now,
            "prev_value": prev_value, "prev_quality": prev_quality,
            "changed": prev_value != v,
        }
    finally:
        con.close()


def list_prefix(prefix: str = "") -> dict:
    con = _conn()
    try:
        out = {}
        q = "SELECT tag, value, quality, updated_at FROM tag_values"
        params: tuple = ()
        if prefix:
            q += " WHERE tag LIKE ?"
            params = (prefix + "%",)
        for tag, value, quality, updated_at in con.execute(q, params):
            out[tag] = {"value": value, "quality": quality, "updated_at": updated_at}
        return {"ok": True, "count": len(out), "tags": out}
    finally:
        con.close()
