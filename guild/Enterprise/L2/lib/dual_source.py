"""
dual_source — fetch the same endpoint from both the teslasolar fork and
the aicraftspeopleguild origin so the SCADA layer can report
side-by-side state.

The key helper pair() returns { mine: <json>, origin: <json>, ... }
with per-side timings so dashboards can render "mine vs origin" cards
without each script rolling its own fetch logic.
"""
import json, time, urllib.request
from typing import Any

import site_base


def _get(url: str, timeout: float = 8.0) -> dict[str, Any]:
    started = time.time()
    try:
        with urllib.request.urlopen(url, timeout=timeout) as r:
            body = r.read().decode("utf-8", errors="replace")
        try:
            return {"ok": True, "url": url, "status": 200,
                    "body": json.loads(body), "ms": int((time.time()-started)*1000)}
        except json.JSONDecodeError:
            return {"ok": True, "url": url, "status": 200,
                    "body": body, "ms": int((time.time()-started)*1000)}
    except Exception as e:
        return {"ok": False, "url": url, "error": f"{type(e).__name__}: {e}",
                "ms": int((time.time()-started)*1000)}


def pair(path: str, timeout: float = 8.0) -> dict[str, Any]:
    """Fetch `path` (joined onto each base) from both mine() and origin()."""
    path = "/" + path.lstrip("/")
    return {
        "path":   path,
        "mine":   _get(site_base.mine()   + path, timeout),
        "origin": _get(site_base.origin() + path, timeout),
    }


def diff(path: str, selector: str | None = None, timeout: float = 8.0) -> dict[str, Any]:
    """Return pair() plus a `same` boolean telling whether a selector's
    value is equal on both sides. Selector is a dotted path into the
    JSON body (e.g. "paperCount" or "enterprise.paperCount.value").
    """
    out = pair(path, timeout)
    out["same"] = False
    if not (out["mine"].get("ok") and out["origin"].get("ok")):
        return out
    m_val, o_val = out["mine"]["body"], out["origin"]["body"]
    if selector:
        for seg in selector.split("."):
            if isinstance(m_val, dict): m_val = m_val.get(seg)
            else: m_val = None
            if isinstance(o_val, dict): o_val = o_val.get(seg)
            else: o_val = None
    out["selected"] = {"mine": m_val, "origin": o_val}
    out["same"] = (m_val == o_val)
    return out


if __name__ == "__main__":
    import sys
    p = sys.argv[1] if len(sys.argv) > 1 else "/guild/Enterprise/L4/api/health.json"
    print(json.dumps(pair(p), indent=2))
