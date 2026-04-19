#!/usr/bin/env python3
"""
pull-instances — fetch the fork's live papers.json and seed a
WhitepaperApp UDT instance per paper at
  guild/Enterprise/L3/udts/whitepaper-app/instances/<slug>.json

- Existing files are NOT overwritten unless --force is passed, so you
  can hand-tweak a paper (theme, abstract, extra copy) and keep those
  edits across re-pulls.
- `label` defaults to a human PascalCase of the slug.
- `android_app_id` / `ios_bundle_id` get the slug appended (with dashes
  stripped) so they're unique per paper.
- `theme_color_hex` rotates a palette derived from the guild charter
  colors so each paper gets a distinct hue but the set stays coherent.
- Funky title/abstract strings (stray leading/trailing backslashes,
  doubled quotes from an older Markdown-to-JSON pipeline) get
  cleaned.

Usage
-----
    python phone/whitepapers/_generator/pull-instances.py
    python phone/whitepapers/_generator/pull-instances.py --force
    python phone/whitepapers/_generator/pull-instances.py --host https://aicraftspeopleguild.github.io
"""
import argparse, json, re, sys, urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent          # teslasolar/phone/whitepapers/_generator
REPO = HERE.parents[2]                          # teslasolar/
UDT_DIR = REPO / "guild" / "Enterprise" / "L3" / "udts" / "whitepaper-app" / "instances"

DEFAULT_HOST = "https://teslasolar.github.io/aicraftspeopleguild.github.io"
API_PATH = "/guild/Enterprise/L4/api/papers.json"

# Small palette — charter green first, then alternating accent hues.
PALETTE = [
    "#1A5C4C", "#c47a20", "#a371f7", "#79c0ff", "#4a8868",
    "#b85c5c", "#6f42c1", "#e3b341", "#f0883e", "#3fb950",
    "#8d95a0", "#1c2128",
]

SLUG_SAFE = re.compile(r"[^a-z0-9]+")

# Windows paths max out at 260 chars, and the generator already adds
# ~180 for the full .../phone/whitepapers/<slug>/android/app/src/main/
# java/com/aicraftspeopleguild/acg/papers/MainActivity.kt tail — so
# the slug directory itself needs to stay well under 60 chars.
SLUG_MAX = 42


def pascal(slug: str) -> str:
    return "".join(w.capitalize() for w in SLUG_SAFE.split(slug) if w) or slug


def bundle_suffix(slug: str) -> str:
    # iOS / Android app-id components must match /^[A-Za-z][\w-]*$/ per
    # segment. We just drop dashes and lowercase.
    return SLUG_SAFE.sub("", slug) or "paper"


def short_slug(full: str, taken: set[str]) -> str:
    """Truncate to SLUG_MAX chars, trim to the last word boundary, and
    append -2/-3/... if two papers collapse to the same prefix."""
    base = full[:SLUG_MAX].rstrip("-")
    if "-" in base and len(full) > SLUG_MAX:
        base = base.rsplit("-", 1)[0]  # trim partial trailing word
    if base not in taken:
        taken.add(base); return base
    i = 2
    while f"{base}-{i}" in taken: i += 1
    out = f"{base}-{i}"; taken.add(out); return out


def clean(s: str | None) -> str:
    if not s: return ""
    s = s.strip()
    # Strip stray backslashes from over-escaped JSON (seen on some papers).
    while s.startswith("\\") or s.startswith('"\\'): s = s.lstrip("\\\"").strip()
    while s.endswith("\\")   or s.endswith('\\"'):   s = s.rstrip("\\\"").strip()
    # Collapse `\"` that survived round-tripping back into `"`.
    s = s.replace('\\"', '"')
    return s


def fetch_papers(host: str) -> list[dict]:
    url = host.rstrip("/") + API_PATH
    with urllib.request.urlopen(url, timeout=15) as r:
        return json.loads(r.read().decode("utf-8", errors="replace"))


def make_instance(paper: dict, theme_hex: str, taken: set[str]) -> dict:
    raw = (paper.get("slug") or paper.get("id") or "").strip().lower()
    raw = SLUG_SAFE.sub("-", raw).strip("-") or "paper"
    slug = short_slug(raw, taken)
    title = clean(paper.get("title"))
    date  = clean(paper.get("date") or paper.get("publication_date"))
    return {
        "udtType":  "WhitepaperApp",
        "instance": slug,
        "parameters": {
            "slug":            slug,
            "title":           title or pascal(slug),
            "author":          clean(paper.get("author")),
            "date":            date,
            "doc_number":      clean(paper.get("doc_number") or paper.get("id")),
            "abstract":        clean(paper.get("abstract")),
            "paper_url":       f"https://teslasolar.github.io/aicraftspeopleguild.github.io/#/whitepapers/{slug}",
            "android_package": "com.aicraftspeopleguild.acg.papers",
            "android_app_id":  f"com.aicraftspeopleguild.acg.papers.{bundle_suffix(slug)}",
            "ios_bundle_id":   f"com.aicraftspeopleguild.acg.papers.{bundle_suffix(slug)}",
            "theme_color_hex": theme_hex,
            "label":           pascal(slug)[:16] or slug,
        },
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--host",  default=DEFAULT_HOST, help="Pages host to pull papers.json from")
    ap.add_argument("--force", action="store_true",  help="overwrite existing instance files")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    papers = fetch_papers(args.host)
    print(f"fetched {len(papers)} paper(s) from {args.host}{API_PATH}")

    UDT_DIR.mkdir(parents=True, exist_ok=True)

    created, updated, skipped = 0, 0, 0
    taken: set[str] = set()
    for i, p in enumerate(papers):
        theme = PALETTE[i % len(PALETTE)]
        inst  = make_instance(p, theme, taken)
        slug  = inst["instance"]
        path  = UDT_DIR / f"{slug}.json"
        exists = path.exists()
        if exists and not args.force:
            skipped += 1
            print(f"  = {slug}  (exists, --force to overwrite)")
            continue
        payload = json.dumps(inst, indent=2, ensure_ascii=False) + "\n"
        if args.dry_run:
            print(f"  {'+' if not exists else '~'} {slug}  [dry-run]")
        else:
            path.write_text(payload, encoding="utf-8")
            if exists: updated += 1
            else:      created += 1
            print(f"  {'+' if not exists else '~'} {slug}")

    print(f"done · +{created} created · ~{updated} updated · ={skipped} skipped")
    return 0


if __name__ == "__main__":
    sys.exit(main())
