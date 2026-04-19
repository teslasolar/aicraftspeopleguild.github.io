#!/usr/bin/env python3
"""Fork vs origin compare — one SVG card with three rows (heartbeat,
paperCount, last-commit). Left column = mine (the teslasolar fork),
right column = origin (aicraftspeopleguild). A checkmark/X between the
two columns shows same/drift.

Run locally:
    python guild/Enterprise/L4/svg/build-fork-compare.py
With custom hosts:
    SITE_BASE=https://teslasolar.github.io/.../ ORIGIN_BASE=... python ...
"""
import json, sys, urllib.request
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(REPO / "guild" / "Enterprise" / "L2" / "lib"))
import site_base
import svg_widget as S
import dual_source

OUT = REPO / "guild" / "Enterprise" / "L2" / "hmi" / "web" / "assets" / "svg" / "fork-compare.svg"


def _last_commit(repo_slug: str) -> dict:
    url = f"https://api.github.com/repos/{repo_slug}/commits?per_page=1"
    try:
        with urllib.request.urlopen(url, timeout=6) as r:
            arr = json.loads(r.read().decode("utf-8", errors="replace"))
        if isinstance(arr, list) and arr:
            c = arr[0]
            return {"sha": c.get("sha","")[:7],
                    "when": (c.get("commit",{}).get("author",{}).get("date") or "")[:10],
                    "msg":  (c.get("commit",{}).get("message") or "").splitlines()[0][:60]}
    except Exception as e:
        return {"err": f"{type(e).__name__}"}
    return {}


def _pick(body, selector: str):
    cur = body
    for seg in selector.split("."):
        if isinstance(cur, dict): cur = cur.get(seg)
        else: return None
    return cur


def render() -> str:
    mine_base   = site_base.mine()
    origin_base = site_base.origin()

    health = dual_source.pair("/guild/Enterprise/L4/api/health.json")
    tags   = dual_source.pair("/guild/Enterprise/L4/runtime/tags.json")

    m_paper = _pick(health["mine"].get("body"),   "paperCount") if health["mine"].get("ok")   else None
    o_paper = _pick(health["origin"].get("body"), "paperCount") if health["origin"].get("ok") else None

    m_hb = _pick(tags["mine"].get("body"),   "enterprise.heartbeat.value") if tags["mine"].get("ok")   else None
    o_hb = _pick(tags["origin"].get("body"), "enterprise.heartbeat.value") if tags["origin"].get("ok") else None

    mine_slug   = mine_base.split("://",1)[-1].split("/")[0].split(".")[0] + "/aicraftspeopleguild.github.io"
    origin_slug = "aicraftspeopleguild/aicraftspeopleguild.github.io"
    mine_commit   = _last_commit(mine_slug)
    origin_commit = _last_commit(origin_slug)

    W, H = 1040, 260
    col_w = 420
    mid_x = W // 2
    left_x, right_x = 40, W - col_w - 40

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" role="img" aria-label="Fork vs origin compare">',
        S.shared_defs(),
        S.window_chrome(W, H, f"⚒ fork compare · mine={mine_base.split('//',1)[-1]}  ·  origin={origin_base.split('//',1)[-1]}"),
    ]

    # Column headers
    parts.append(f'<text x="{left_x}"  y="64" class="t vs">mine  ·  teslasolar</text>')
    parts.append(f'<text x="{right_x}" y="64" class="t vs">origin  ·  aicraftspeopleguild</text>')

    rows = [
        ("heartbeat",   str(m_hb) if m_hb is not None else "—",
                        str(o_hb) if o_hb is not None else "—",
                        m_hb == o_hb and m_hb is not None),
        ("paperCount",  str(m_paper) if m_paper is not None else "—",
                        str(o_paper) if o_paper is not None else "—",
                        m_paper == o_paper and m_paper is not None),
        ("last commit", f"{mine_commit.get('sha','—')} · {mine_commit.get('when','')}" + (f" · {mine_commit.get('msg','')}" if mine_commit.get('msg') else ""),
                        f"{origin_commit.get('sha','—')} · {origin_commit.get('when','')}" + (f" · {origin_commit.get('msg','')}" if origin_commit.get('msg') else ""),
                        mine_commit.get("sha") and mine_commit.get("sha") == origin_commit.get("sha")),
    ]

    y = 96
    for label, m, o, same in rows:
        parts.append(f'<text x="{left_x}"  y="{y}" class="t lb">{label}</text>')
        parts.append(f'<text x="{left_x}"  y="{y+20}" class="t vs">{S.esc(m)[:56]}</text>')
        parts.append(f'<text x="{right_x}" y="{y}" class="t lb">{label}</text>')
        parts.append(f'<text x="{right_x}" y="{y+20}" class="t vs">{S.esc(o)[:56]}</text>')
        marker = "=" if same else "≠"
        color  = S.P["green"] if same else S.P["amber"]
        parts.append(f'<text x="{mid_x}" y="{y+18}" text-anchor="middle" class="t" font-size="22" font-weight="700" fill="{color}">{marker}</text>')
        y += 52

    parts.append(f'<text x="{W-24}" y="{H-18}" text-anchor="end" class="t sub">generated {datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%MZ")}</text>')
    parts.append("</svg>")
    return "\n".join(parts)


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(render(), encoding="utf-8")
    print(f"[fork-compare] wrote {OUT.relative_to(REPO)}")


if __name__ == "__main__":
    main()
