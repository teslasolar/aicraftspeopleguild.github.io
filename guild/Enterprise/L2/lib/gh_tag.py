"""
gh_tag — use GitHub Issues as a dynamic tag database.

Tag ↔ Issue mapping
-------------------
  title    tag:<path>                e.g. "tag:live.sprint-counter"
  body     JSON { value, quality, updated_at, type, description }
  labels   tag, ns:<namespace>       e.g. ns:live, ns:enterprise
  history  each comment is a new Value/Quality/Timestamp record

Read path uses the unauthenticated GitHub API (60 req/hr public rate
limit). Writes require GITHUB_TOKEN env var (PAT with `public_repo` or
`repo` scope). Target repo defaults to
aicraftspeopleguild/aicraftspeopleguild.github.io and can be overridden
via GH_TAG_REPO env.
"""
import json, os, urllib.request, urllib.parse
from datetime import datetime, timezone

REPO = os.environ.get("GH_TAG_REPO", "aicraftspeopleguild/aicraftspeopleguild.github.io")
API  = "https://api.github.com"
TOK  = os.environ.get("GITHUB_TOKEN", "").strip()


def _now():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _req(method: str, path: str, body=None) -> dict:
    url = f"{API}{path}"
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "acg-gh-tag/1.0"}
    if TOK:
        headers["Authorization"] = f"Bearer {TOK}"
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=12) as r:
            txt = r.read().decode("utf-8", errors="replace")
            return json.loads(txt) if txt else {}
    except urllib.error.HTTPError as e:
        try:
            detail = json.loads(e.read().decode("utf-8", errors="replace"))
        except Exception:
            detail = {"status": e.code}
        raise RuntimeError(f"github {e.code}: {detail.get('message') or detail}") from e


def _parse_body(body: str) -> dict:
    if not body:
        return {}
    # strip fenced code block if present
    t = body.strip()
    if t.startswith("```"):
        t = t.split("\n", 1)[1] if "\n" in t else t
        if t.endswith("```"):
            t = t[:-3]
        if t.lstrip().startswith("json"):
            t = t.lstrip()[4:].lstrip()
    try:
        return json.loads(t)
    except Exception:
        return {"raw": body}


def _fence(d: dict) -> str:
    return "```json\n" + json.dumps(d, indent=2, ensure_ascii=False) + "\n```"


def _find_issue(path: str) -> dict | None:
    """Search by exact title match tag:<path>."""
    title = f"tag:{path}"
    q = urllib.parse.quote(f'repo:{REPO} in:title "{title}" is:issue')
    data = _req("GET", f"/search/issues?q={q}&per_page=5")
    for item in data.get("items") or []:
        if item.get("title") == title:
            return item
    return None


# ── public API ──────────────────────────────────────────────────────────

def read(path: str = "") -> dict:
    if not path:
        return {"ok": False, "error": "path required"}
    iss = _find_issue(path)
    if not iss:
        return {"ok": False, "error": f"no tag issue for '{path}'"}
    # Latest value = last comment if any, else issue body
    latest_body = iss.get("body", "")
    comments_url = iss.get("comments_url")
    if iss.get("comments", 0) and comments_url:
        cs = _req("GET", comments_url[len(API):] + "?per_page=100")
        if isinstance(cs, list) and cs:
            latest_body = cs[-1].get("body", latest_body)
    v = _parse_body(latest_body)
    return {
        "ok":         True,
        "path":       path,
        "issue":      iss.get("number"),
        "url":        iss.get("html_url"),
        "comments":   iss.get("comments", 0),
        "updated_at": v.get("updated_at") or iss.get("updated_at"),
        "value":      v.get("value"),
        "quality":    v.get("quality", "good"),
        "type":       v.get("type"),
        "description": v.get("description"),
    }


def list_tags(ns: str = "") -> dict:
    labels = "tag" + (f",ns:{ns}" if ns else "")
    data = _req("GET", f"/repos/{REPO}/issues?labels={labels}&state=open&per_page=100")
    if not isinstance(data, list):
        return {"ok": False, "error": str(data)}
    out = []
    for iss in data:
        title = iss.get("title", "")
        if not title.startswith("tag:"):
            continue
        v = _parse_body(iss.get("body", ""))
        out.append({
            "path":    title[4:],
            "issue":   iss.get("number"),
            "url":     iss.get("html_url"),
            "value":   v.get("value"),
            "quality": v.get("quality", "good"),
            "updated_at": iss.get("updated_at"),
            "comments": iss.get("comments", 0),
        })
    return {"ok": True, "count": len(out), "tags": out}


def write(path: str = "", value=None, quality: str = "good",
          type: str = "String", description: str = "") -> dict:
    """Append a new comment with the current Value/Quality/Timestamp."""
    if not path:    return {"ok": False, "error": "path required"}
    if not TOK:     return {"ok": False, "error": "GITHUB_TOKEN required to write"}

    iss = _find_issue(path)
    payload = {
        "value":       value,
        "quality":     quality,
        "updated_at":  _now(),
        "type":        type,
    }
    if description: payload["description"] = description

    if iss is None:
        # Init: create issue with body
        created = _req("POST", f"/repos/{REPO}/issues", {
            "title":  f"tag:{path}",
            "body":   _fence(payload),
            "labels": ["tag"] + ([f"ns:{path.split('.')[0]}"] if "." in path else []),
        })
        return {"ok": True, "op": "init", "issue": created.get("number"), "url": created.get("html_url"), "path": path, "value": value}

    num = iss["number"]
    # Append a comment (history) AND update issue body so read() without
    # comment paging still sees the latest value.
    _req("POST", f"/repos/{REPO}/issues/{num}/comments", {"body": _fence(payload)})
    _req("PATCH", f"/repos/{REPO}/issues/{num}", {"body": _fence(payload)})
    return {"ok": True, "op": "update", "issue": num, "url": iss.get("html_url"), "path": path, "value": value}


def init(path: str = "", value=None, quality: str = "good",
         type: str = "String", description: str = "") -> dict:
    """Explicit init: fails if issue already exists (unlike write which auto-updates)."""
    if _find_issue(path):
        return {"ok": False, "error": f"tag '{path}' already exists"}
    return write(path, value, quality, type, description)
