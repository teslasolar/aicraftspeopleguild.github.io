#!/usr/bin/env python3
"""
build-konomi.py — aggregate guild/Enterprise/docs/standards/konomi/ into a
single L4 API bundle that both Pages and the phone apps consume.

Outputs:
  guild/Enterprise/L4/api/konomi.json        — full bundle (meta + base + standards + crosswalks)
  guild/Enterprise/L4/api/konomi/index.json  — catalog (just counts + STD ids)
  guild/Enterprise/L4/api/konomi/stds/<id>.json — one per standard

The source files under docs/standards/konomi/ remain canonical; this
script is a pure projection so the API never drifts from the data.
"""
from __future__ import annotations
import json, sys, os, pathlib, datetime, re

ROOT = pathlib.Path(__file__).resolve().parents[5]
SRC  = ROOT / "guild" / "Enterprise" / "docs" / "standards" / "konomi"
OUT  = ROOT / "guild" / "Enterprise" / "L4" / "api" / "konomi.json"
CAT  = ROOT / "guild" / "Enterprise" / "L4" / "api" / "konomi"
CAT.mkdir(parents=True, exist_ok=True)
(CAT / "stds").mkdir(parents=True, exist_ok=True)

# ignore files that are bookkeeping rather than content
SKIP_NAMES = {"path.json", "udt.db"}

def load(p: pathlib.Path) -> dict | None:
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"WARN  {p.relative_to(ROOT)}: {e}", file=sys.stderr)
        return None

def scan_std_dir(d: pathlib.Path) -> dict:
    """Collect a single standard's data from one dir."""
    out = {"id": d.name, "_std": None, "udts": [], "state_machines": [],
           "entities": [], "rules": [], "relations": [], "levels": []}
    for f in sorted(d.iterdir()):
        if not f.is_file() or f.name in SKIP_NAMES: continue
        data = load(f)
        if data is None: continue
        name = f.name
        if name == "_std.json":
            out["_std"] = data
        elif name.endswith(".udt.json"):
            out["udts"].append(data)
        elif name.endswith(".sm.json"):
            out["state_machines"].append(data)
        elif name.endswith(".entity.json"):
            out["entities"].append(data)
        elif name.endswith(".rule.json"):
            out["rules"].append(data)
        elif name.endswith(".relation.json"):
            out["relations"].append(data)
        elif name.endswith(".level.json"):
            out["levels"].append(data)
    return out

def main() -> int:
    if not SRC.exists():
        print(f"FAIL  source missing: {SRC}", file=sys.stderr)
        return 1

    meta_schema = load(SRC / "_std.json") or {}
    base = {}
    for f in sorted((SRC / "base").iterdir()):
        if f.name.endswith(".udt.json"):
            data = load(f)
            if data: base[data.get("name", f.stem)] = data

    standards: dict[str, dict] = {}
    for d in sorted(SRC.iterdir()):
        if not d.is_dir(): continue
        if d.name in ("base", "crosswalks", "meta"): continue
        std = scan_std_dir(d)
        if std["_std"] or std["udts"]:
            standards[d.name] = std

    crosswalks = []
    cw_dir = SRC / "crosswalks"
    if cw_dir.exists():
        for f in sorted(cw_dir.iterdir()):
            if f.name.startswith("_") or f.name in SKIP_NAMES: continue
            data = load(f)
            if data: crosswalks.append(data)

    bundle = {
        "_meta": {
            "name":         "Konomi",
            "version":      meta_schema.get("version", "1.0.0"),
            "scope":        meta_schema.get("scope", ""),
            "motto":        meta_schema.get("motto", ""),
            "generated_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "source":       "guild/Enterprise/docs/standards/konomi/",
            "paper":        "guild/Enterprise/L4/api/white-papers/originals/konomi-standard.md",
            "counts": {
                "base_udts":    len(base),
                "standards":    len(standards),
                "udts":         sum(len(s["udts"]) for s in standards.values()),
                "state_machines": sum(len(s["state_machines"]) for s in standards.values()),
                "crosswalks":   len(crosswalks),
            },
        },
        "schema":     meta_schema,
        "base":       base,
        "standards":  standards,
        "crosswalks": crosswalks,
    }

    OUT.write_text(json.dumps(bundle, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)} · {OUT.stat().st_size:,} bytes")

    # catalog (light)
    catalog = {
        "_meta":      bundle["_meta"],
        "base_udts":  sorted(base.keys()),
        "standards": {
            sid: {
                "id":        s["_std"].get("id")    if s["_std"] else sid,
                "scope":     s["_std"].get("scope") if s["_std"] else "",
                "version":   s["_std"].get("version","") if s["_std"] else "",
                "udt_count": len(s["udts"]),
                "sm_count":  len(s["state_machines"]),
                "href":      f"stds/{sid}.json",
            } for sid, s in standards.items()
        },
        "crosswalks": [{"from_std": c.get("from_std",""), "to_std": c.get("to_std",""),
                        "maps": len(c.get("maps", []))} for c in crosswalks],
    }
    (CAT / "index.json").write_text(json.dumps(catalog, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"wrote {(CAT / 'index.json').relative_to(ROOT)}")

    for sid, s in standards.items():
        p = CAT / "stds" / f"{sid}.json"
        p.write_text(json.dumps(s, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"wrote {len(standards)} per-std files under {(CAT / 'stds').relative_to(ROOT)}")

    return 0

if __name__ == "__main__":
    sys.exit(main())
