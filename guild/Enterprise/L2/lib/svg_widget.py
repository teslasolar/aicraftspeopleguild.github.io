"""
svg_widget — tiny, zero-dep primitives for dashboard SVGs. Mirrors
what the fuller upstream library used to ship — pared down to what the
mirror/compare layer actually needs.
"""
from datetime import datetime, timezone


P = {
    "bg_a":   "#0d1117",
    "bg_b":   "#161b22",
    "chrome": "#161b22",
    "border": "#30363d",
    "text":   "#e6edf3",
    "dim":    "#8b949e",
    "green":  "#3fb950",
    "blue":   "#79c0ff",
    "purple": "#a371f7",
    "amber":  "#e3b341",
    "red":    "#f85149",
    "orange": "#f0883e",
}


def esc(s) -> str:
    return (str(s) if s is not None else "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def pretty_ts(value) -> str:
    try:
        return datetime.fromtimestamp(int(str(value)), tz=timezone.utc).strftime("%Y-%m-%d %H:%MZ")
    except Exception:
        return str(value) if value is not None else "—"


def shared_defs() -> str:
    return f"""<defs>
    <linearGradient id="gBg" x1="0" x2="1" y1="0" y2="1">
      <stop offset="0" stop-color="{P['bg_a']}"/>
      <stop offset="1" stop-color="{P['bg_b']}"/>
    </linearGradient>
    <style>
      .t{{font-family:'SFMono-Regular',Consolas,monospace}}
      .lb{{fill:{P['dim']};font-size:10px;text-transform:uppercase;letter-spacing:1.6px}}
      .va{{fill:{P['text']};font-size:20px;font-weight:700}}
      .vs{{fill:{P['text']};font-size:14px;font-weight:600}}
      .sub{{fill:{P['dim']};font-size:11px}}
      .ok{{fill:{P['green']}}} .er{{fill:{P['red']}}}
    </style>
  </defs>"""


def window_chrome(w: int, h: int, title: str = "") -> str:
    return (
        f'<rect width="{w}" height="{h}" rx="10" fill="url(#gBg)"/>'
        f'<rect x="0" y="0" width="{w}" height="36" rx="10" fill="{P["chrome"]}"/>'
        f'<rect x="0" y="26" width="{w}" height="10" fill="{P["chrome"]}"/>'
        f'<circle cx="22" cy="18" r="6" fill="#ff5f56"/>'
        f'<circle cx="44" cy="18" r="6" fill="#ffbd2e"/>'
        f'<circle cx="66" cy="18" r="6" fill="#27c93f"/>'
        f'<text x="88" y="22" class="t sub">{esc(title)}</text>'
    )


def panel(x: int, y: int, w: int, h: int) -> str:
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="{P["chrome"]}" stroke="{P["border"]}"/>'


def chip(x: int, y: int, text: str, color: str = "green") -> str:
    hex_ = P.get(color, P["green"])
    width = 14 + 7 * len(str(text))
    return (
        f'<rect x="{x}" y="{y}" width="{width}" height="18" rx="9" '
        f'fill="{hex_}22" stroke="{hex_}"/>'
        f'<text x="{x + width // 2}" y="{y + 13}" text-anchor="middle" class="t" '
        f'font-size="10" fill="{hex_}">{esc(text)}</text>'
    )
