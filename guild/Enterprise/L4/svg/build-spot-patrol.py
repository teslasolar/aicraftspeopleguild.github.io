#!/usr/bin/env python3
"""SPOT patrol — robot-dog police officer card. Runs spot.patrol and
renders the results as spot-patrol.svg: a little dog silhouette with a
badge, then one row per beat (green/amber/red dot + label + detail).

Run locally:
    python guild/Enterprise/L4/svg/build-spot-patrol.py

Env:
    SPOT_MANIFESTO_SHEET_ID — Google Sheets id for the manifesto beat
    SITE_BASE / ORIGIN_BASE / GH_TAG_REPO — same as fork-compare
"""
from __future__ import annotations

import json, sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(REPO / "guild" / "Enterprise" / "L2" / "lib"))
sys.path.insert(0, str(REPO / "guild" / "Enterprise" / "L2" / "scada" / "spot"))

import svg_widget as S          # noqa: E402
import patrol                   # noqa: E402
import bark                     # noqa: E402

OUT = REPO / "guild" / "Enterprise" / "L2" / "hmi" / "web" / "assets" / "svg" / "spot-patrol.svg"

# Status → svg_widget color class
_CLASS = {"ok": "g", "warn": "a", "alarm": "r"}
_LAMP  = {"ok": "#3fb950", "warn": "#e3b341", "alarm": "#f85149"}


def _spot(x: int, y: int, scale: float = 1.0, lamp: str = _LAMP["ok"]) -> str:
    """Tiny SPOT silhouette (body + head + legs + ear + tail + badge).
    Origin (x, y) is the top-left of a ~110×70 unscaled bounding box.
    The 'lamp' on the badge colors with the worst patrol status."""
    s = scale
    def P(px, py): return f"{x + px*s:.1f},{y + py*s:.1f}"
    body_fill   = "#e6edf3"
    body_stroke = "#30363d"
    joint_fill  = "#8b949e"
    return f"""<g aria-label="SPOT patrol dog">
    <!-- tail -->
    <path d="M{P(0,32)} Q{P(-10,24)} {P(-6,14)}" stroke="{body_fill}" stroke-width="{5*s:.1f}" fill="none" stroke-linecap="round"/>
    <!-- body -->
    <rect x="{x:.1f}" y="{y+26*s:.1f}" width="{70*s:.1f}" height="{22*s:.1f}" rx="{6*s:.1f}" fill="{body_fill}" stroke="{body_stroke}" stroke-width="1.5"/>
    <!-- legs (front/back pairs) -->
    <rect x="{x+6*s:.1f}"  y="{y+46*s:.1f}" width="{8*s:.1f}" height="{18*s:.1f}" rx="2" fill="{joint_fill}"/>
    <rect x="{x+18*s:.1f}" y="{y+46*s:.1f}" width="{8*s:.1f}" height="{22*s:.1f}" rx="2" fill="{body_fill}"/>
    <rect x="{x+46*s:.1f}" y="{y+46*s:.1f}" width="{8*s:.1f}" height="{22*s:.1f}" rx="2" fill="{body_fill}"/>
    <rect x="{x+58*s:.1f}" y="{y+46*s:.1f}" width="{8*s:.1f}" height="{18*s:.1f}" rx="2" fill="{joint_fill}"/>
    <!-- head + neck -->
    <path d="M{P(60,30)} L{P(78,14)} L{P(104,14)} L{P(104,34)} L{P(72,34)} Z" fill="{body_fill}" stroke="{body_stroke}" stroke-width="1.5"/>
    <!-- ear -->
    <path d="M{P(80,14)} L{P(76,4)} L{P(90,12)} Z" fill="{body_stroke}"/>
    <!-- eye -->
    <circle cx="{x+94*s:.1f}" cy="{y+22*s:.1f}" r="{1.8*s:.1f}" fill="#0d1117"/>
    <!-- nose -->
    <rect x="{x+100*s:.1f}" y="{y+24*s:.1f}" width="{4*s:.1f}" height="{4*s:.1f}" fill="#0d1117"/>
    <!-- police badge (shield) on flank -->
    <path d="M{P(30,30)} L{P(46,30)} L{P(46,40)} Q{P(38,50)} {P(30,40)} Z" fill="#79c0ff" stroke="#0d1117" stroke-width="1"/>
    <text x="{x+38*s:.1f}" y="{y+39*s:.1f}" text-anchor="middle" font-family="'SFMono-Regular',Consolas,monospace" font-size="{8*s:.1f}" font-weight="700" fill="#0d1117">K9</text>
    <!-- status lamp on top of head -->
    <circle cx="{x+90*s:.1f}" cy="{y+10*s:.1f}" r="{3.2*s:.1f}" fill="{lamp}"/>
    <circle cx="{x+90*s:.1f}" cy="{y+10*s:.1f}" r="{5.5*s:.1f}" fill="none" stroke="{lamp}" stroke-width="1.2" opacity=".45" class="pulse"/>
  </g>"""


_LAST_REPORT: dict = {}


def render() -> str:
    global _LAST_REPORT
    report = patrol.run()
    _LAST_REPORT = report
    beats = report["beats"]

    # Layout
    W = 1040
    row_h = 40
    header_h = 130
    footer_h = 46
    H = header_h + row_h * max(len(beats), 1) + footer_h

    worst_lamp = _LAMP.get(report["worst"], _LAMP["ok"])
    title = (f"🐕 SPOT patrol · mine→origin sweep · "
             f"ok={report['summary']['ok']} warn={report['summary']['warn']} "
             f"alarm={report['summary']['alarm']}")

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
        f'width="{W}" height="{H}" role="img" aria-label="SPOT patrol report">',
        S.shared_defs(),
        S.window_chrome(W, H, title),
        _spot(28, 52, scale=1.0, lamp=worst_lamp),
        f'<text x="160" y="72" class="t vs" font-size="16" font-weight="700" '
        f'fill="{S.P["text"]}">SPOT · SCADA patrol agent</text>',
        f'<text x="160" y="92" class="t sub">teslasolar treats origin as the plant; '
        f'SPOT walks a beat every sweep and barks on drift, broken links, '
        f'prompt-injection, exposed secrets, manifesto tampering, and '
        f'IEC 61131-3 tag-type violations.</text>',
        f'<text x="160" y="112" class="t sub">last sweep {report["generated"]} '
        f'· duration {report["duration_ms"]}ms · worst status '
        f'<tspan fill="{worst_lamp}" font-weight="700">{report["worst"].upper()}</tspan></text>',
        S.divider(40, header_h - 6, W - 80),
    ]

    y = header_h + 18
    for r in beats:
        cls = _CLASS.get(r["status"], "a")
        parts.append(S.pulse_dot(62, y - 4, cls, r=6, ring_r=10))
        parts.append(
            f'<text x="92" y="{y}" class="t vs">{S.esc(r["label"])}</text>'
        )
        det = r["detail"] if len(r["detail"]) <= 110 else r["detail"][:109] + "…"
        parts.append(
            f'<text x="92" y="{y+16}" class="t sub">{S.esc(det)}</text>'
        )
        parts.append(S.chip(W - 160, y, r["status"].upper(), cls, min_w=68))
        parts.append(
            f'<text x="{W-40}" y="{y}" text-anchor="end" class="t sub">{r["ms"]}ms</text>'
        )
        y += row_h

    parts.append(S.divider(40, H - footer_h + 10, W - 80))
    parts.append(
        f'<text x="40" y="{H-14}" class="t sub">beats: fork-drift · link-health · '
        f'paper-injection · paper-crypto · manifesto-sheet · isa-61131</text>'
    )
    parts.append(
        f'<text x="{W-24}" y="{H-14}" text-anchor="end" class="t sub">'
        f'{report["generated"]}</text>'
    )
    parts.append("</svg>")
    return "\n".join(parts)


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(render(), encoding="utf-8")
    patrol.LOG_OUT.parent.mkdir(parents=True, exist_ok=True)
    patrol.LOG_OUT.write_text(json.dumps(_LAST_REPORT, indent=2), encoding="utf-8")
    b = bark.sync(_LAST_REPORT["beats"])
    print(f"[spot-patrol] wrote {OUT.relative_to(REPO)}")
    print(f"[spot-patrol] wrote {patrol.LOG_OUT.relative_to(REPO)} "
          f"(worst={_LAST_REPORT['worst']})")
    print(f"[spot-patrol] bark: raised={b['raised']} cleared={b['cleared']} kept={b['kept']}"
          + (f" skipped_no_db={b['skipped_no_db']}" if b.get("skipped_no_db") else ""))


if __name__ == "__main__":
    main()
