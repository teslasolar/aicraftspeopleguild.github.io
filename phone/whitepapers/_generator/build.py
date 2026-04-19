#!/usr/bin/env python3
"""
build — read WhitepaperApp UDT instances and render the _template/
tree into phone/whitepapers/<slug>/{android,ios}/ for each paper.

Token substitution is plain str.replace — no Jinja, no deps. Tokens
recognised inside both file content AND file names:

    {{SLUG}}              kebab-case slug, e.g. flywheel
    {{SLUG_PASCAL}}       PascalCase, e.g. Flywheel
    {{TITLE}}             paper title
    {{AUTHOR}}            author string
    {{DATE}}              any date string
    {{DOC_NUMBER}}        doc id
    {{ABSTRACT}}          one- or two-paragraph teaser
    {{PAPER_URL}}         deep link into the site
    {{ANDROID_PACKAGE}}   Kotlin package / Android namespace
    {{ANDROID_APP_ID}}    applicationId (must be unique per paper)
    {{IOS_BUNDLE_ID}}     iOS bundle id (unique per paper)
    {{THEME_COLOR_HEX}}   primary color (#RRGGBB)
    {{LABEL}}             launcher label · defaults to SLUG_PASCAL

Usage
-----
    python _generator/build.py                # render every instance
    python _generator/build.py --only flywheel
    python _generator/build.py --dry-run      # print plan, write nothing
    python _generator/build.py --list         # list discovered instances

Output
------
    phone/whitepapers/<slug>/android/         (gradle project)
    phone/whitepapers/<slug>/ios/             (XcodeGen project)
"""
import argparse, json, re, shutil, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent          # teslasolar/phone/whitepapers/_generator
REPO = HERE.parents[2]                          # teslasolar/
UDT_DIR  = REPO / "guild" / "Enterprise" / "L3" / "udts" / "whitepaper-app" / "instances"
TEMPLATE = HERE.parent / "_template"
MINIS    = HERE.parent / "_minis"               # paper-specific interactive overlays
OUT_ROOT = HERE.parent                          # phone/whitepapers/

BINARY_SUFFIXES = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".jar"}


def _pascal(slug: str) -> str:
    return re.sub(r"(^|[-_ ])([a-z0-9])", lambda m: m.group(2).upper(), slug.lower())


def _tokens(params: dict) -> dict[str, str]:
    slug  = params["slug"]
    label = params.get("label") or _pascal(slug)
    return {
        "{{SLUG}}":             slug,
        "{{SLUG_PASCAL}}":      _pascal(slug),
        "{{TITLE}}":            params.get("title", slug),
        "{{AUTHOR}}":           params.get("author", ""),
        "{{DATE}}":             params.get("date", ""),
        "{{DOC_NUMBER}}":       params.get("doc_number", ""),
        "{{ABSTRACT}}":         params.get("abstract", ""),
        "{{PAPER_URL}}":        params.get("paper_url", ""),
        "{{BODY_URL}}":         params.get("body_url", params.get("paper_url", "")),
        "{{ANDROID_PACKAGE}}":  params.get("android_package", "com.aicraftspeopleguild.acg.papers"),
        "{{ANDROID_APP_ID}}":   params.get("android_app_id", f"com.aicraftspeopleguild.acg.papers.{slug.replace('-', '')}"),
        "{{IOS_BUNDLE_ID}}":    params.get("ios_bundle_id", f"com.aicraftspeopleguild.acg.papers.{slug.replace('-', '')}"),
        "{{THEME_COLOR_HEX}}":  params.get("theme_color_hex", "#1A5C4C"),
        "{{LABEL}}":            label,
        "{{MINI_APP}}":         params.get("mini_app", ""),
    }


def _apply(text: str, tokens: dict[str, str]) -> str:
    for k, v in tokens.items():
        text = text.replace(k, v)
    # XML / strings.xml require escaping ampersands + quotes
    return text


def _xml_escape(s: str) -> str:
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
             .replace('"', "&quot;").replace("'", "&apos;"))


def _is_binary(path: Path) -> bool:
    return path.suffix.lower() in BINARY_SUFFIXES


def render_tree(src: Path, dst: Path, tokens: dict[str, str], dry: bool) -> int:
    """Copy src → dst, token-replacing file contents AND path segments.
    String values used inside XML/strings.xml get xml-escaped before
    substitution so <, >, &, ", ' don't blow up the parser."""
    written = 0
    for s in src.rglob("*"):
        rel = s.relative_to(src)
        rel_str = _apply(str(rel).replace("\\", "/"), tokens)
        target = dst / rel_str

        if s.is_dir():
            if not dry: target.mkdir(parents=True, exist_ok=True)
            continue

        if not dry:
            target.parent.mkdir(parents=True, exist_ok=True)

        if _is_binary(s):
            if not dry: shutil.copy2(s, target)
            written += 1
            continue

        raw = s.read_text(encoding="utf-8")
        # For .xml files, escape token values defensively. Same text
        # inside .kt / .swift is quoted literals, which also accept &<>
        # but we escape for xml files only to be safe.
        if s.suffix == ".xml":
            xml_tokens = {k: _xml_escape(v) for k, v in tokens.items()}
            rendered = _apply(raw, xml_tokens)
        elif s.suffix in {".yml", ".yaml"}:
            # YAML double-quoted scalars: escape backslash and double-quote.
            yml_tokens = {k: v.replace("\\", "\\\\").replace('"', '\\"')
                          for k, v in tokens.items()}
            rendered = _apply(raw, yml_tokens)
        else:
            rendered = _apply(raw, tokens)

        if not dry:
            target.write_text(rendered, encoding="utf-8")
        written += 1
    return written


def load_instances() -> list[dict]:
    out = []
    for f in sorted(UDT_DIR.glob("*.json")):
        if f.name.startswith("_"): continue
        doc = json.loads(f.read_text(encoding="utf-8"))
        if doc.get("udtType") != "WhitepaperApp": continue
        out.append(doc.get("parameters") or {})
    return out


def build_one(params: dict, dry: bool) -> None:
    tokens = _tokens(params)
    slug   = params["slug"]
    out    = OUT_ROOT / slug
    print(f"-> {slug}")
    n_a = render_tree(TEMPLATE / "android", out / "android", tokens, dry)
    n_i = render_tree(TEMPLATE / "ios",     out / "ios",     tokens, dry)
    # If the UDT instance names a mini, overlay that mini's source
    # over the just-rendered template. Overlays match file paths 1:1,
    # so the mini's MiniRegistry.kt replaces the template stub.
    mini = (params.get("mini_app") or "").strip()
    n_m_a = n_m_i = 0
    if mini:
        mdir_a = MINIS / mini / "android"
        mdir_i = MINIS / mini / "ios"
        if mdir_a.exists(): n_m_a = render_tree(mdir_a, out / "android", tokens, dry)
        if mdir_i.exists(): n_m_i = render_tree(mdir_i, out / "ios",     tokens, dry)
    extra = f"  overlay(mini={mini}): {n_m_a}+{n_m_i}" if mini else ""
    print(f"  wrote {n_a} android · {n_i} ios file(s){extra}{'  (dry-run)' if dry else ''}")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--only",    help="slug to render (default: all)")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--list",    action="store_true", help="list slugs and exit")
    args = ap.parse_args()

    instances = load_instances()
    if args.list:
        for p in instances:
            print(f"  {p['slug']:28}  {p.get('title','')}")
        return 0
    if not instances:
        print("no WhitepaperApp instances found under", UDT_DIR)
        return 2

    target = [p for p in instances if (not args.only) or p["slug"] == args.only]
    if not target:
        print(f"no match for --only {args.only!r}"); return 2
    for p in target:
        build_one(p, dry=args.dry_run)
    print(f"done · {len(target)} app pair(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
