#!/usr/bin/env python3
"""
build-umbrella — aggregate every WhitepaperApp UDT instance's
parameters into one papers.json bundled into the umbrella app. Run
after pull-instances.py to keep the umbrella in sync.

Writes:
  phone/whitepapers/_umbrella/android/app/src/main/assets/papers.json
  phone/whitepapers/_umbrella/ios/ACGPapers/Resources/papers.json
"""
import json, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
INST = REPO / "guild" / "Enterprise" / "L3" / "udts" / "whitepaper-app" / "instances"
OUT_ANDROID = REPO / "phone" / "whitepapers" / "_umbrella" / "android" / "app" / "src" / "main" / "assets" / "papers.json"
OUT_IOS     = REPO / "phone" / "whitepapers" / "_umbrella" / "ios"     / "ACGPapers" / "Resources" / "papers.json"

# Fields the umbrella cares about — keep the payload small.
FIELDS = ["slug","title","author","date","doc_number","abstract","paper_url","theme_color_hex"]


def main() -> int:
    papers = []
    for f in sorted(INST.glob("*.json")):
        if f.name.startswith("_"): continue
        doc = json.loads(f.read_text(encoding="utf-8"))
        if doc.get("udtType") != "WhitepaperApp": continue
        p = doc.get("parameters") or {}
        papers.append({k: p.get(k, "") for k in FIELDS})

    payload = json.dumps(papers, indent=2, ensure_ascii=False) + "\n"
    for out in (OUT_ANDROID, OUT_IOS):
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(payload, encoding="utf-8")
        print(f"wrote {out.relative_to(REPO)}  ({len(papers)} papers)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
