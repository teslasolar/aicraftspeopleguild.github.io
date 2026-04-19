"""spot.beats — individual patrol checks ("beats") that SPOT walks on
every sweep. A beat is a callable that takes a config dict and returns:

    {"id", "label", "status": "ok"|"warn"|"alarm", "detail", "ms", "extra"}

Status ladder:
  ok     green — sensor reads clean
  warn   amber — drift or suspicious-but-not-confirmed
  alarm  red   — concrete violation (exposed secret, injected prompt,
                 manifesto tampering, dead link on origin)

All fetches go through the existing dual_source / urllib helpers so the
same TTL/timeout behaviour as fork-compare applies.
"""
from __future__ import annotations

import csv, hashlib, io, json, re, sys, time, urllib.request
from pathlib import Path
from typing import Any, Callable

# Sibling lib access — mirrors build-fork-compare.py's sys.path push.
_REPO = Path(__file__).resolve().parents[5]
sys.path.insert(0, str(_REPO / "guild" / "Enterprise" / "L2" / "lib"))
import site_base                        # noqa: E402
import dual_source                      # noqa: E402


# ── helpers ─────────────────────────────────────────────────────────────

def _get(url: str, timeout: float = 8.0) -> dict[str, Any]:
    t0 = time.time()
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "spot-patrol/1"})
        with urllib.request.urlopen(req, timeout=timeout) as r:
            body = r.read().decode("utf-8", errors="replace")
            return {"ok": True, "status": r.status, "body": body,
                    "ms": int((time.time() - t0) * 1000)}
    except Exception as e:
        return {"ok": False, "error": f"{type(e).__name__}: {e}",
                "ms": int((time.time() - t0) * 1000)}


def _result(bid: str, label: str, status: str, detail: str,
            ms: int = 0, **extra) -> dict[str, Any]:
    return {"id": bid, "label": label, "status": status,
            "detail": detail, "ms": ms, "extra": extra or None}


def _trunc(s: str, n: int) -> str:
    s = str(s)
    return s if len(s) <= n else s[: max(0, n - 1)] + "…"


# Baselines (per-beat hashes/sizes) persist across sweeps in the runtime
# tree so the GH Actions checkout picks them up next run.
_BASELINES_FILE = _REPO / "guild" / "Enterprise" / "L4" / "runtime" / "spot-baselines.json"


def _baselines() -> dict:
    try:
        return json.loads(_BASELINES_FILE.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return {}
    except Exception:
        return {}


def _baselines_save(store: dict) -> None:
    _BASELINES_FILE.parent.mkdir(parents=True, exist_ok=True)
    _BASELINES_FILE.write_text(json.dumps(store, indent=2, sort_keys=True),
                               encoding="utf-8")


# ── injection / crypto pattern banks ────────────────────────────────────
# Kept small on purpose — SPOT is a tripwire, not a full scanner.

_INJECTION_PATTERNS = [
    re.compile(r"ignore (?:all |the )?previous (?:instructions|prompt)s?", re.I),
    re.compile(r"disregard (?:the )?(?:above|system) (?:prompt|message)", re.I),
    re.compile(r"you are now (?:a |an )?(?:DAN|jailbroken|unrestricted)", re.I),
    re.compile(r"\[\[\s*system\s*\]\]", re.I),
    re.compile(r"<\s*\|?\s*(?:im_start|system)\s*\|?\s*>", re.I),
    re.compile(r"reveal (?:your )?system prompt", re.I),
]

_SECRET_PATTERNS = [
    ("aws_access_key",  re.compile(r"\bAKIA[0-9A-Z]{16}\b")),
    ("github_token",    re.compile(r"\bghp_[A-Za-z0-9]{30,}\b")),
    ("openai_key",      re.compile(r"\bsk-[A-Za-z0-9]{20,}\b")),
    ("anthropic_key",   re.compile(r"\bsk-ant-[A-Za-z0-9_-]{30,}\b")),
    ("private_key_pem", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")),
    ("generic_jwt",     re.compile(r"\beyJ[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}\.[A-Za-z0-9_\-]{10,}\b")),
]

# Tag type vocabulary — the runtime uses an ACG dialect (Counter,
# DateTime, String, …) rather than raw IEC 61131-3 elementary types.
# We keep a mapping so SPOT can (a) certify each ACG type maps to a
# 61131-3 type, and (b) flag anything outside the known vocabulary as
# a rogue tag declaration.
_ACG_TO_61131 = {
    "Counter":   "UDINT",
    "Gauge":     "REAL",
    "Number":    "REAL",
    "Integer":   "DINT",
    "Boolean":   "BOOL",
    "String":    "STRING",
    "DateTime":  "DATE_AND_TIME",
    "Duration":  "TIME",
    "Enum":      "STRING",
    "Json":      "STRING",
}
# Raw 61131-3 elementary types are also accepted (for PLC-authored tags).
_IEC_61131_TYPES = {
    "BOOL", "BYTE", "WORD", "DWORD", "LWORD",
    "SINT", "INT", "DINT", "LINT",
    "USINT", "UINT", "UDINT", "ULINT",
    "REAL", "LREAL",
    "STRING", "WSTRING",
    "TIME", "DATE", "TIME_OF_DAY", "DATE_AND_TIME",
}


# ── beats ───────────────────────────────────────────────────────────────

def origin_heartbeat(cfg: dict) -> dict:
    """Fetch origin's root HTML, track size + sha256 across sweeps. This
    is the fork's primary "plant up" sensor — a 200 with a body that
    hashes to the baseline means origin is serving what it served last
    time. First sweep records baseline; subsequent sweeps compare.
    """
    t0 = time.time()
    base = cfg.get("origin") or site_base.origin()
    r = _get(base + "/", timeout=float(cfg.get("timeout", 8)))
    ms = int((time.time() - t0) * 1000)
    if not r["ok"]:
        return _result("origin-heartbeat", "origin heartbeat", "alarm",
                       f"unreachable: {r.get('error')}", ms)

    body = r["body"]
    size = len(body.encode("utf-8"))
    sha = hashlib.sha256(body.encode("utf-8")).hexdigest()[:12]

    store = _baselines()
    prev = store.get("origin-heartbeat") or {}
    store["origin-heartbeat"] = {"sha": sha, "size": size,
                                 "at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}
    _baselines_save(store)

    if not prev:
        return _result("origin-heartbeat", "origin heartbeat", "ok",
                       f"baselined  sha={sha}  size={size}b", ms,
                       sha=sha, size=size, baseline=True)
    if prev.get("sha") == sha:
        return _result("origin-heartbeat", "origin heartbeat", "ok",
                       f"stable  sha={sha}  size={size}b", ms,
                       sha=sha, size=size)
    # Content changed — not necessarily bad (the site updates). Warn
    # so the log records it; operator decides whether to re-baseline.
    delta = size - int(prev.get("size") or 0)
    return _result("origin-heartbeat", "origin heartbeat", "warn",
                   f"changed  sha {prev.get('sha')}→{sha}  Δ{delta:+d}b",
                   ms, sha=sha, prev=prev.get("sha"), size=size)


def link_health(cfg: dict) -> dict:
    """GET each URL in cfg['urls']; any non-2xx == alarm."""
    urls = cfg.get("urls") or [site_base.origin() + "/"]
    broken, slow = [], []
    t0 = time.time()
    for u in urls:
        r = _get(u, timeout=float(cfg.get("timeout", 6)))
        if not r["ok"] or not (200 <= r.get("status", 0) < 400):
            broken.append(u.rsplit("/", 1)[-1] or u)
        elif r["ms"] > int(cfg.get("slow_ms", 2500)):
            slow.append(f"{u.rsplit('/',1)[-1] or u}={r['ms']}ms")
    ms = int((time.time() - t0) * 1000)
    if broken:
        return _result("link-health", "link patrol", "alarm",
                       f"{len(broken)} broken: {', '.join(broken[:3])}", ms,
                       broken=broken)
    if slow:
        return _result("link-health", "link patrol", "warn",
                       f"slow: {', '.join(slow[:3])}", ms, slow=slow)
    return _result("link-health", "link patrol", "ok",
                   f"{len(urls)} links 2xx", ms)


def _strip_html(s: str) -> str:
    # Kill script/style bodies first so regex doesn't match their text.
    s = re.sub(r"<script\b[^>]*>.*?</script>", " ", s, flags=re.S | re.I)
    s = re.sub(r"<style\b[^>]*>.*?</style>",   " ", s, flags=re.S | re.I)
    s = re.sub(r"<!--.*?-->", " ", s, flags=re.S)
    s = re.sub(r"<[^>]+>", " ", s)
    return s


def html_scan(cfg: dict) -> dict:
    """Origin is a flat HTML site. Fetch each URL in cfg['pages'] (paths
    relative to origin base) and scan both for prompt-injection
    patterns and leaked secrets. One beat, one sweep, both coverage."""
    base = cfg.get("origin") or site_base.origin()
    pages = cfg.get("pages") or ["/"]
    t0 = time.time()
    fetched = 0
    injection_hits: list[dict] = []
    secret_hits: list[dict] = []
    unreachable: list[str] = []

    for path in pages:
        url = base + path if path.startswith("/") else base + "/" + path
        r = _get(url, timeout=float(cfg.get("timeout", 8)))
        if not r["ok"]:
            unreachable.append(path)
            continue
        fetched += 1
        text = _strip_html(r["body"])
        for pat in _INJECTION_PATTERNS:
            m = pat.search(text)
            if m:
                injection_hits.append({"page": path,
                                       "match": _trunc(m.group(0), 48)})
                break
        for name, pat in _SECRET_PATTERNS:
            m = pat.search(r["body"])   # scan raw body for secrets
            if m:
                secret_hits.append({"page": path, "kind": name,
                                    "sample": _trunc(m.group(0), 12)})

    ms = int((time.time() - t0) * 1000)

    if secret_hits:
        first = secret_hits[0]
        return _result("html-scan", "origin HTML scan", "alarm",
                       f"{len(secret_hits)} secret(s) leaked, "
                       f"e.g. {first['kind']} in {first['page']}",
                       ms, secrets=secret_hits, injection=injection_hits,
                       fetched=fetched, unreachable=unreachable)
    if injection_hits:
        first = injection_hits[0]
        return _result("html-scan", "origin HTML scan", "alarm",
                       f"{len(injection_hits)} prompt-injection hit(s), "
                       f"e.g. {first['page']}: \"{first['match']}\"",
                       ms, injection=injection_hits,
                       fetched=fetched, unreachable=unreachable)
    if unreachable and not fetched:
        return _result("html-scan", "origin HTML scan", "alarm",
                       f"0/{len(pages)} pages reachable", ms,
                       unreachable=unreachable)
    if unreachable:
        return _result("html-scan", "origin HTML scan", "warn",
                       f"{fetched}/{len(pages)} clean, "
                       f"{len(unreachable)} unreachable",
                       ms, unreachable=unreachable, fetched=fetched)
    return _result("html-scan", "origin HTML scan", "ok",
                   f"{fetched}/{len(pages)} pages clean "
                   f"(no injection, no secrets)", ms, fetched=fetched)


def manifesto_hash(cfg: dict) -> dict:
    """Tamper-watch a single document (default: the manifesto HTML).
    First sweep records sha256 into spot-baselines.json; subsequent
    sweeps compare. Any mismatch == alarm (potential tampering).
    Operator can re-baseline by clearing the key from the baselines
    file or passing cfg.force_rebaseline=true."""
    base = cfg.get("origin") or site_base.origin()
    path = cfg.get("path", "/aicraftspeopleguild-manifesto.html")
    t0 = time.time()
    r = _get(base + path, timeout=float(cfg.get("timeout", 8)))
    ms = int((time.time() - t0) * 1000)
    if not r["ok"]:
        return _result("manifesto-hash", "manifesto tamper-watch", "alarm",
                       f"unreachable: {r.get('error')}", ms)

    sha = hashlib.sha256(r["body"].encode("utf-8")).hexdigest()
    size = len(r["body"].encode("utf-8"))
    store = _baselines()
    key = f"manifesto-hash:{path}"
    prev = store.get(key) or {}

    if cfg.get("force_rebaseline") or not prev:
        store[key] = {"sha": sha, "size": size,
                      "at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}
        _baselines_save(store)
        return _result("manifesto-hash", "manifesto tamper-watch", "ok",
                       f"baselined  sha={sha[:12]}  size={size}b", ms,
                       sha=sha[:12], size=size, baseline=True)

    if prev.get("sha") == sha:
        return _result("manifesto-hash", "manifesto tamper-watch", "ok",
                       f"unchanged  sha={sha[:12]}  since {prev.get('at','?')}",
                       ms, sha=sha[:12], since=prev.get("at"))
    delta = size - int(prev.get("size") or 0)
    return _result("manifesto-hash", "manifesto tamper-watch", "alarm",
                   f"TAMPER  sha {prev.get('sha','?')[:12]}→{sha[:12]}  Δ{delta:+d}b",
                   ms, sha=sha[:12], prev=prev.get("sha"), since=prev.get("at"))


def manifesto_sheet(cfg: dict) -> dict:
    """Watch a Google Sheets published-CSV for the manifesto sign-on list.
    cfg keys:
      sheet_id   — required (or SPOT_MANIFESTO_SHEET_ID env)
      gid        — optional sub-sheet id (default 0)
      min_rows   — minimum expected row count; drop below == alarm
    """
    import os
    sheet_id = cfg.get("sheet_id") or os.environ.get("SPOT_MANIFESTO_SHEET_ID", "").strip()
    if not sheet_id:
        return _result("manifesto-sheet", "manifesto sheet", "warn",
                       "no sheet_id configured (set SPOT_MANIFESTO_SHEET_ID)")

    gid = str(cfg.get("gid", "0"))
    url = f"https://docs.google.com/spreadsheets/d/{sheet_id}/export?format=csv&gid={gid}"
    t0 = time.time()
    r = _get(url, timeout=float(cfg.get("timeout", 10)))
    ms = int((time.time() - t0) * 1000)
    if not r["ok"]:
        return _result("manifesto-sheet", "manifesto sheet", "alarm",
                       f"unreachable: {r.get('error')}", ms)
    body = r["body"].lstrip()
    # Unpublished / private sheets redirect to a sign-in page (HTML),
    # which csv.reader happily parses as a 1-column sheet. Sniff first.
    if body.startswith("<") or "<html" in body[:200].lower():
        return _result("manifesto-sheet", "manifesto sheet", "alarm",
                       "sheet not published to web (got HTML sign-in)",
                       ms)
    try:
        rows = list(csv.reader(io.StringIO(r["body"])))
    except Exception as e:
        return _result("manifesto-sheet", "manifesto sheet", "warn",
                       f"csv parse: {type(e).__name__}", ms)
    # Header + signers
    signers = max(0, len(rows) - 1)
    min_rows = int(cfg.get("min_rows", 1))
    if signers < min_rows:
        return _result("manifesto-sheet", "manifesto sheet", "alarm",
                       f"only {signers} signer(s), expected ≥{min_rows}",
                       ms, signers=signers)
    return _result("manifesto-sheet", "manifesto sheet", "ok",
                   f"{signers} signer(s)", ms, signers=signers)


def isa_61131_tags(cfg: dict) -> dict:
    """Self-check of the fork's own sensor layer. Origin doesn't publish
    tags (no SCADA surface upstream — SPOT itself is the control
    layer), so this beat validates the fork's runtime/tags.json: every
    tag must use either an ACG vocabulary type (Counter, DateTime, …)
    OR a raw IEC-61131-3 elementary type. Anything outside both ==
    warn (rogue tag declaration inside the plant)."""
    t0 = time.time()
    base = cfg.get("target") or site_base.mine()
    r = _get(base + "/guild/Enterprise/L4/runtime/tags.json",
             timeout=float(cfg.get("timeout", 8)))
    ms = int((time.time() - t0) * 1000)
    if not r["ok"]:
        return _result("isa-61131", "IEC 61131-3 tag types", "warn",
                       f"fetch failed: {r.get('error')}", ms)
    try:
        body = json.loads(r["body"])
    except json.JSONDecodeError:
        return _result("isa-61131", "IEC 61131-3 tag types", "warn",
                       "tags.json not JSON", ms)

    bad, total = [], 0

    def walk(prefix: str, node: Any):
        nonlocal total
        if isinstance(node, dict):
            # Tag leaf: has at least a "type" and a "value"/"quality".
            if "type" in node and ("value" in node or "quality" in node):
                total += 1
                raw = str(node.get("type", ""))
                if raw not in _ACG_TO_61131 and raw.upper() not in _IEC_61131_TYPES:
                    bad.append({"tag": prefix, "type": raw or "<empty>"})
                return
            for k, v in node.items():
                if k.startswith("_"):   # skip _meta etc.
                    continue
                walk(f"{prefix}.{k}" if prefix else k, v)

    walk("", body)
    if not total:
        return _result("isa-61131", "IEC 61131-3 tag types", "ok",
                       "no typed tags to validate", ms)
    if bad:
        first = bad[0]
        return _result("isa-61131", "IEC 61131-3 tag types", "warn",
                       f"{len(bad)}/{total} rogue type, e.g. {first['tag']}={first['type']}",
                       ms, bad=bad[:10], total=total)
    return _result("isa-61131", "IEC 61131-3 tag types", "ok",
                   f"{total}/{total} typed (ACG→61131 mapped)", ms)


# Registry — the patrol runner looks up beats by name from beats.json.
REGISTRY: dict[str, Callable[[dict], dict]] = {
    "origin-heartbeat": origin_heartbeat,
    "link-health":      link_health,
    "html-scan":        html_scan,
    "manifesto-hash":   manifesto_hash,
    "manifesto-sheet":  manifesto_sheet,
    "isa-61131":        isa_61131_tags,
}
