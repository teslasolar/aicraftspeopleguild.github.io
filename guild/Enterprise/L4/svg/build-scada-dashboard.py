#!/usr/bin/env python3
# @tag-event
# {
#   "id": "build-scada-dashboard-svg:on-heartbeat",
#   "listens": {"kind":"on_transition","tag":"demo.heartbeat","from":"*","to":"CHANGED"},
#   "writes": ["guild/Enterprise/L2/hmi/web/assets/svg/scada-dashboard.svg"]
# }
# @end-tag-event
"""
Full SCADA dashboard — one SVG, one glass cockpit. Eight framed panels
read from the current public endpoints + state.db + spot-patrol.json +
GitHub Issues. Composed from the atomic svg_widget primitives.

Layout (1040 × 1200):
  0. HERO              window chrome · title · aggregate plant lamp · heartbeat
  1. CATALOG           4 stat_blocks from /api/health.json
  2. ENTERPRISE        6 stat_blocks from /runtime/tags.json
  3. PIPELINE          complete / aborted / events / success + sparkline + bar
  4. SPOT PATROL       robot-dog status + 6 beat rows from spot-patrol.json
  5. IDENTITY          host · deployMode · apiVersion · latestPaper chips
  6. TAGS              up to 8 live GitHub-Issue tag cards
  7. FAULTS            active faults from /api/state.json
"""
import json, sys, sqlite3, urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(REPO / "guild" / "Enterprise" / "L2" / "lib"))
import svg_widget as S
import gh_tag

OUT  = REPO / "guild" / "Enterprise" / "L2" / "hmi" / "web" / "assets" / "svg" / "scada-dashboard.svg"
BASE = "https://teslasolar.github.io/aicraftspeopleguild.github.io"
SPOT_LOCAL = REPO / "guild" / "Enterprise" / "L4" / "api" / "spot-patrol.json"

# Visual palette shared with svg_widget — pull into locals for brevity.
P = S.P
LAMP = {"ok": P["green"], "warn": P["amber"], "alarm": P["red"],
        "good": P["green"]}


def _get(url):
    try:
        with urllib.request.urlopen(url, timeout=8) as r:
            return json.loads(r.read().decode("utf-8", errors="replace"))
    except Exception:
        return {}


def _durations():
    f = REPO / "guild" / "Enterprise" / "L2" / "state" / "state.db"
    if not f.exists(): return []
    try:
        con = sqlite3.connect(str(f))
        rows = con.execute(
            "SELECT duration_s FROM pipeline_runs ORDER BY started_at DESC LIMIT 20"
        ).fetchall()
        con.close()
        return [float(r[0] or 0) for r in reversed(rows)]
    except Exception:
        return []


def _spot_report() -> dict:
    # Prefer the committed JSON (pages-served), fall back to local file.
    remote = _get(f"{BASE}/guild/Enterprise/L4/api/spot-patrol.json")
    if remote and remote.get("beats"):
        return remote
    try:
        return json.loads(SPOT_LOCAL.read_text(encoding="utf-8"))
    except Exception:
        return {"worst": "ok", "summary": {"ok": 0, "warn": 0, "alarm": 0},
                "beats": [], "generated": "—", "duration_ms": 0}


# ── panel chrome ─────────────────────────────────────────────────────

def panel_frame(x: int, y: int, w: int, h: int, title: str,
                lamp_class: str = "g", sub: str = "") -> list[str]:
    """Rounded panel with an integrated header strip: LED + title (left),
    optional subtitle (right). Returns the list of SVG fragments; the
    caller fills the body from y + 34 onward."""
    body_top = y + 28
    return [
        # outer frame
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" '
        f'fill="{P["bg_b"]}" stroke="{P["border"]}" stroke-width="1"/>',
        # header strip
        f'<rect x="{x}" y="{y}" width="{w}" height="26" rx="8" '
        f'fill="{P["bg_a"]}" stroke="{P["border"]}" stroke-width="1"/>',
        f'<rect x="{x}" y="{y+18}" width="{w}" height="10" '
        f'fill="{P["bg_a"]}"/>',
        # divider below header
        f'<line x1="{x}" y1="{body_top-2}" x2="{x+w}" y2="{body_top-2}" '
        f'stroke="{P["border"]}" stroke-width="1"/>',
        # LED
        f'<circle cx="{x+14}" cy="{y+13}" r="4" '
        f'fill="{LAMP.get(lamp_class, P["green"])}"/>',
        # title
        f'<text x="{x+26}" y="{y+17}" class="t" font-size="11" '
        f'font-weight="700" fill="{P["text"]}" letter-spacing="1.2">'
        f'{S.esc(title.upper())}</text>',
        # subtitle right-aligned
        (f'<text x="{x+w-10}" y="{y+17}" text-anchor="end" class="t sub" '
         f'font-size="11">{S.esc(sub)}</text>' if sub else ""),
    ]


# ── main render ──────────────────────────────────────────────────────

def render() -> str:
    health    = _get(f"{BASE}/guild/Enterprise/L4/api/health.json")
    tags      = _get(f"{BASE}/guild/Enterprise/L4/runtime/tags.json")
    state     = _get(f"{BASE}/guild/Enterprise/L4/api/state.json")
    hb        = gh_tag.read("demo.heartbeat")
    gh_tags   = gh_tag.list_tags().get("tags", []) or []
    durations = _durations()
    spot      = _spot_report()

    def tv(obj, key, default=0):
        v = obj.get(key, {})
        return v.get("value", default) if isinstance(v, dict) else default

    enterprise = tags.get("enterprise", {}) if isinstance(tags, dict) else {}
    pipeline   = tags.get("pipeline",   {}) if isinstance(tags, dict) else {}
    identity   = tags.get("identity",   {}) if isinstance(tags, dict) else {}
    catalog    = tags.get("catalog",    {}) if isinstance(tags, dict) else {}

    # Aggregate plant state — worst of (spot.worst, faults_active>0).
    faults_list = (state.get("faults_active") if isinstance(state, dict) else []) or []
    faults_n    = len(faults_list)
    spot_worst  = spot.get("worst", "ok")
    agg_rank    = {"ok": 0, "warn": 1, "alarm": 2, "error": 2}
    plant = "alarm" if faults_n and any((f.get("severity") or "").lower() == "error"
                                        for f in faults_list) else spot_worst
    if plant not in agg_rank:
        plant = "warn" if faults_n else "ok"

    W = 1040
    M = 20                       # outer margin
    GAP = 14                     # inter-panel gap

    parts = []
    # Header
    parts.append(f'<svg xmlns="http://www.w3.org/2000/svg" '
                 f'viewBox="0 0 {W} __H__" width="{W}" role="img" '
                 f'aria-label="ACG SCADA dashboard">')
    parts.append(S.shared_defs())
    # Placeholder for chrome — filled after we know the height.
    chrome_slot = len(parts)
    parts.append("")

    # Plant-state mega lamp (top-right) + heartbeat
    plant_color = LAMP.get(plant, P["green"])
    hb_label = S.pretty_ts(hb.get("value")) if hb.get("ok") else "offline"
    parts.append(f'<g transform="translate({W-230}, 16)">')
    parts.append(f'<rect x="0" y="-4" width="220" height="26" rx="13" '
                 f'fill="{P["bg_a"]}" stroke="{plant_color}" stroke-width="1.2"/>')
    parts.append(f'<circle cx="16" cy="9" r="6" fill="{plant_color}"/>')
    parts.append(f'<circle cx="16" cy="9" r="10" fill="none" stroke="{plant_color}" '
                 f'stroke-width="1.2" opacity=".45" class="pulse"/>')
    parts.append(f'<text x="32" y="13" class="t" font-size="11" font-weight="700" '
                 f'fill="{plant_color}" letter-spacing="1.6">PLANT {plant.upper()}</text>')
    parts.append(f'<text x="210" y="13" text-anchor="end" class="t sub" '
                 f'font-size="11">💓 {S.esc(hb_label)}</text>')
    parts.append('</g>')

    y = 48  # first panel starts below header

    # ────── CATALOG ─────────────────────────────────────────────────
    cat_h = 130
    parts += panel_frame(M, y, W - 2*M, cat_h,
                         "CATALOG · /api/health.json", "g",
                         f"papers {health.get('paperCount','—')} · "
                         f"api v{health.get('apiVersion','—')}")
    body_y = y + 40
    sw, sh, gap = 232, 84, 10
    cat_rows = [
        ("papers",     health.get("paperCount", "—"), "g"),
        ("members",    health.get("memberCount", "—"), "o"),
        ("api",        f"v{health.get('apiVersion', '—')}", "p"),
        ("lastBuild",  (health.get("lastUpdated") or "—")[-20:-1] if health.get("lastUpdated") else "—", "g"),
    ]
    for i, (lbl, val, color) in enumerate(cat_rows):
        x = M + 14 + i * (sw + gap)
        parts.append(S.stat_block(x, body_y, sw, sh, lbl, val, "", color))
    y += cat_h + GAP

    # ────── ENTERPRISE ─────────────────────────────────────────────
    ent_h = 126
    parts += panel_frame(M, y, W - 2*M, ent_h,
                         "ENTERPRISE · runtime/tags.json", "o",
                         f"{len(enterprise)} tags")
    body_y = y + 40
    sw, sh, gap = 156, 76, 8
    ent_rows = [
        ("paperCount",    tv(enterprise, "paperCount"),    "g"),
        ("memberCount",   tv(enterprise, "memberCount"),   "o"),
        ("programCount",  tv(enterprise, "programCount"),  "p"),
        ("runCount",      tv(enterprise, "runCount"),      "g"),
        ("tagEdges",      tv(enterprise, "tagEdges"),      "p"),
        ("authoredLinks", tv(enterprise, "authoredLinks"), "o"),
    ]
    for i, (lbl, val, color) in enumerate(ent_rows):
        x = M + 14 + i * (sw + gap)
        parts.append(S.stat_block(x, body_y, sw, sh, lbl, val, "", color))
    y += ent_h + GAP

    # ────── PIPELINE ───────────────────────────────────────────────
    pipe_h = 134
    complete = tv(pipeline, "complete", 0)
    aborted  = tv(pipeline, "aborted", 0)
    states   = tv(pipeline, "states", 0)
    total    = (complete or 0) + (aborted or 0)
    rate     = int(100 * (complete or 0) / total) if total else 100
    parts += panel_frame(M, y, W - 2*M, pipe_h,
                         "PIPELINE · L3 runs",
                         "r" if aborted else "g",
                         f"{complete} ok · {aborted} aborted · {rate}% success")
    body_y = y + 38
    parts.append(S.pulse_dot(M + 30, body_y + 20, "r" if aborted else "g", r=7, ring_r=11))
    parts.append(f'<text x="{M+54}" y="{body_y + 16}" class="t lb">complete</text>')
    parts.append(f'<text x="{M+54}" y="{body_y + 42}" class="t va g">{complete}</text>')
    parts.append(f'<text x="{M+178}" y="{body_y + 16}" class="t lb">aborted</text>')
    parts.append(f'<text x="{M+178}" y="{body_y + 42}" class="t va {"r" if aborted else "g"}">{aborted}</text>')
    parts.append(f'<text x="{M+278}" y="{body_y + 16}" class="t lb">events</text>')
    parts.append(f'<text x="{M+278}" y="{body_y + 42}" class="t va">{states}</text>')
    parts.append(f'<text x="{M+378}" y="{body_y + 16}" class="t lb">success</text>')
    parts.append(f'<text x="{M+378}" y="{body_y + 42}" class="t va g">{rate}%</text>')
    if durations:
        spx, spy, spw, sph = W - M - 280, body_y + 12, 260, 40
        parts.append(f'<text x="{spx}" y="{body_y+8}" class="t lb">last {len(durations)} runs (s)</text>')
        parts.append(S.sparkline(spx, spy, spw, sph, durations, "g"))
    parts.append(S.bar(M + 14, y + pipe_h - 22, W - 2*M - 28, 8,
                       rate / 100, "g" if rate >= 80 else "a"))
    y += pipe_h + GAP

    # ────── SPOT PATROL ────────────────────────────────────────────
    beats = spot.get("beats") or []
    spot_h = max(196, 52 + 26 * len(beats))
    spot_href = f"{BASE}/guild/Enterprise/L2/scada/spot/"
    parts.append(f'<a href="{S.esc(spot_href)}" target="_blank" aria-label="open SPOT SCADA page">')
    parts += panel_frame(M, y, W - 2*M, spot_h,
                         "SPOT · patrol agent",
                         {"ok":"g","warn":"a","alarm":"r"}.get(spot_worst, "a"),
                         f"sweep {spot.get('duration_ms','—')}ms · "
                         f"ok {spot.get('summary',{}).get('ok',0)} · "
                         f"warn {spot.get('summary',{}).get('warn',0)} · "
                         f"alarm {spot.get('summary',{}).get('alarm',0)} · "
                         f"{spot.get('generated','—')}")
    body_y = y + 40

    # Mini SPOT dog silhouette (left ~88 wide)
    dx, dy = M + 22, body_y + 4
    dog_lamp = LAMP.get(spot_worst, LAMP["ok"])
    parts.append(f'<g aria-label="SPOT">'
                 f'<rect x="{dx+6}" y="{dy+22}" width="64" height="20" rx="6" '
                 f'fill="{P["text"]}" stroke="{P["border"]}" stroke-width="1.2"/>'
                 f'<rect x="{dx+12}" y="{dy+40}" width="7" height="16" rx="2" fill="{P["dim"]}"/>'
                 f'<rect x="{dx+22}" y="{dy+40}" width="7" height="20" rx="2" fill="{P["text"]}"/>'
                 f'<rect x="{dx+46}" y="{dy+40}" width="7" height="20" rx="2" fill="{P["text"]}"/>'
                 f'<rect x="{dx+56}" y="{dy+40}" width="7" height="16" rx="2" fill="{P["dim"]}"/>'
                 f'<path d="M{dx+56},{dy+26} L{dx+72},{dy+14} L{dx+92},{dy+14} L{dx+92},{dy+30} L{dx+66},{dy+30} Z" '
                 f'fill="{P["text"]}" stroke="{P["border"]}" stroke-width="1.2"/>'
                 f'<path d="M{dx+74},{dy+14} L{dx+71},{dy+5} L{dx+82},{dy+12} Z" fill="{P["border"]}"/>'
                 f'<circle cx="{dx+85}" cy="{dy+21}" r="1.6" fill="{P["bg_a"]}"/>'
                 f'<rect x="{dx+90}" y="{dy+22}" width="4" height="4" fill="{P["bg_a"]}"/>'
                 f'<path d="M{dx+28},{dy+26} L{dx+42},{dy+26} L{dx+42},{dy+36} Q{dx+35},{dy+44} {dx+28},{dy+36} Z" '
                 f'fill="{P["blue"]}" stroke="{P["bg_a"]}" stroke-width="1"/>'
                 f'<text x="{dx+35}" y="{dy+35}" text-anchor="middle" class="t" '
                 f'font-size="8" font-weight="700" fill="{P["bg_a"]}">K9</text>'
                 f'<circle cx="{dx+80}" cy="{dy+8}" r="3" fill="{dog_lamp}"/>'
                 f'<circle cx="{dx+80}" cy="{dy+8}" r="5" fill="none" stroke="{dog_lamp}" '
                 f'stroke-width="1" opacity=".5" class="pulse"/>'
                 f'</g>')

    # Beat rows (right of dog)
    rx = M + 126
    row = body_y + 8
    col_detail_w = W - 2*M - 126 - 24 - 120
    for b in beats[:8]:
        status = b.get("status", "ok")
        cls = {"ok": "g", "warn": "a", "alarm": "r"}.get(status, "a")
        parts.append(f'<circle cx="{rx+6}" cy="{row}" r="5" '
                     f'fill="{LAMP.get(status, P["green"])}"/>')
        parts.append(f'<text x="{rx+20}" y="{row+4}" class="t vs" '
                     f'font-size="12">{S.esc(b.get("label") or b.get("id",""))}</text>')
        det = b.get("detail","")
        det = det if len(det) <= 78 else det[:77] + "…"
        parts.append(f'<text x="{rx+200}" y="{row+4}" class="t sub" '
                     f'font-size="11">{S.esc(det)}</text>')
        parts.append(f'<text x="{W-M-20}" y="{row+4}" text-anchor="end" '
                     f'class="t sub" font-size="10">{b.get("ms",0)}ms  '
                     f'<tspan fill="{LAMP.get(status, P["green"])}" font-weight="700">'
                     f'{status.upper()}</tspan></text>')
        row += 22
    parts.append('</a>')
    y += spot_h + GAP

    # ────── IDENTITY chips ─────────────────────────────────────────
    id_h = 64
    parts += panel_frame(M, y, W - 2*M, id_h,
                         "IDENTITY · deploy", "p",
                         f"host {tv(identity, 'host', '—')}")
    row_y = y + 44
    chips = [
        (f'host {tv(identity, "host", "—")}',            "g"),
        (f'deploy {tv(identity, "deployMode", "—")}',    "o"),
        (f'api v{tv(identity, "apiVersion", "1.0")}',    "p"),
        (f'latest {str(tv(catalog, "latestPaperTitle", "—"))[:32]}', "g"),
    ]
    cx = M + 14
    for text, color in chips:
        parts.append(S.chip(cx, row_y, text, color))
        cx += max(60, len(text) * 8 + 20) + 10
    y += id_h + GAP

    # ────── TAGS grid ──────────────────────────────────────────────
    tag_h = 190
    parts += panel_frame(M, y, W - 2*M, tag_h,
                         "TAGS · GitHub Issues", "g",
                         f"{len(gh_tags)} open")
    body_y = y + 40
    cw, ch, gap = 240, 70, 10
    cols = 4
    for i, t in enumerate(gh_tags[:8]):
        r, c = divmod(i, cols)
        x = M + 14 + c * (cw + gap)
        ty = body_y + r * (ch + gap)
        val = str(t.get("value") or "—")[:20]
        parts.append(S.tag_card(x, ty, cw, ch, t["path"], val,
                                "g" if t.get("quality") == "good" else "a",
                                subtitle=f'#{t.get("issue","?")} · {t.get("comments",0)} edits'))
    y += tag_h + GAP

    # ────── FAULTS strip ───────────────────────────────────────────
    faults_h = 74
    summary = state.get("summary") if isinstance(state, dict) else {}
    active_n = (summary.get("faults_active", faults_n)
                if isinstance(summary, dict) else faults_n)
    parts += panel_frame(M, y, W - 2*M, faults_h,
                         "FAULTS · active", "r" if active_n else "g",
                         f"{active_n} active")
    body_y = y + 40
    if faults_list:
        parts.append(S.pulse_dot(M + 30, body_y + 10, "r", r=7, ring_r=11))
        parts.append(f'<text x="{M+54}" y="{body_y+14}" class="t va r" '
                     f'font-size="16">{active_n} active</text>')
        cx = M + 164
        for f in faults_list[:5]:
            msg = f'{f.get("kind","?")} · {(f.get("message") or "")[:36]}'
            parts.append(S.chip(cx, body_y + 14, msg, "r"))
            cx += max(60, len(msg) * 8 + 20) + 10
    else:
        parts.append(S.pulse_dot(M + 30, body_y + 10, "g", r=7, ring_r=11))
        parts.append(f'<text x="{M+54}" y="{body_y+14}" class="t va g" '
                     f'font-size="16">0 active · all quiet</text>')
    y += faults_h

    H = y + 18  # bottom margin

    # Fill in the chrome slot now that we know the final height.
    parts[0] = parts[0].replace("__H__", str(H))
    parts[chrome_slot] = S.window_chrome(W, H, "⚒ ACG · SCADA CONTROL BOARD · LIVE")

    # Subtle horizontal bus line under the hero bar
    parts.append(f'<line x1="0" y1="38" x2="{W}" y2="38" '
                 f'stroke="{P["border"]}" stroke-width="1" opacity=".6"/>')
    parts.append(f'<text x="{W-12}" y="{H-8}" text-anchor="end" class="t sub" '
                 f'font-size="10">regenerates on every push + every 15 minutes</text>')
    parts.append("</svg>")
    return "\n".join(parts)


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(render(), encoding="utf-8")
    print(f"[scada-dashboard] wrote {OUT.relative_to(REPO)}")


if __name__ == "__main__":
    main()
