#!/usr/bin/env python3
"""
udt-catalog — walk guild/Enterprise/L3/udts/ and print one JSON blob
per discovered instance. Handy for shell pipelines + for seeding the
Android app's local UDT registry.

Usage
-----
  python bin/udt-catalog.py              # print everything
  python bin/udt-catalog.py --type Api   # filter by udtType
  python bin/udt-catalog.py --index      # regenerate L3/udts/catalog.json
"""
import argparse, json, sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
UDT  = REPO / "guild" / "Enterprise" / "L3" / "udts"


def walk_instances(udt_type: str | None = None):
    for tpl in sorted(UDT.glob("*/template.json")):
        doc = json.loads(tpl.read_text(encoding="utf-8"))
        t   = doc.get("udtType")
        if udt_type and t != udt_type:
            continue
        inst_dir = tpl.parent / "instances"
        if not inst_dir.exists():
            continue
        for inst in sorted(inst_dir.glob("*.json")):
            if inst.name == "_index.json":
                continue
            try:
                yield json.loads(inst.read_text(encoding="utf-8")) | {
                    "_file": inst.relative_to(REPO).as_posix(),
                }
            except Exception as e:
                yield {"_file": inst.as_posix(), "_error": str(e)}


def regen_index():
    out = {
        "generated": __import__("datetime").datetime.now(
            __import__("datetime").timezone.utc
        ).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "templates": [],
        "instances": {},
    }
    for tpl in sorted(UDT.glob("*/template.json")):
        out["templates"].append(tpl.relative_to(UDT).as_posix())
        doc = json.loads(tpl.read_text(encoding="utf-8"))
        t   = doc.get("udtType") or tpl.parent.name
        files = [p.relative_to(UDT).as_posix()
                 for p in sorted((tpl.parent / "instances").glob("*.json"))
                 if p.name != "_index.json"]
        if files:
            out["instances"].setdefault(t, []).extend(files)
    catalog = UDT / "catalog.json"
    catalog.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {catalog.relative_to(REPO)} · {sum(len(v) for v in out['instances'].values())} instances")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--type",  help="filter by udtType (Tool, Script, Api, MirrorPair)")
    ap.add_argument("--index", action="store_true", help="regenerate L3/udts/catalog.json")
    args = ap.parse_args()
    if args.index:
        regen_index()
        return
    for doc in walk_instances(args.type):
        print(json.dumps(doc, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
