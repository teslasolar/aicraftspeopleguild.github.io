"""spot.patrol — the SCADA patrol runner. Reads beats.json, calls each
beat from spot.beats.REGISTRY, writes a structured JSON log to
guild/Enterprise/L4/api/spot-patrol.json so the Pages site and the
SVG builder can both consume the same data.

Run:
    python -m guild.Enterprise.L2.scada.spot.patrol
    # or
    python guild/Enterprise/L2/scada/spot/patrol.py
"""
from __future__ import annotations

import json, sys, time
from datetime import datetime, timezone
from pathlib import Path

_HERE = Path(__file__).resolve()
_REPO = _HERE.parents[5]
sys.path.insert(0, str(_HERE.parent))
import beats as _beats   # noqa: E402
import bark as _bark     # noqa: E402

MANIFEST = _HERE.parent / "beats.json"
LOG_OUT  = _REPO / "guild" / "Enterprise" / "L4" / "api" / "spot-patrol.json"

STATUS_RANK = {"ok": 0, "warn": 1, "alarm": 2}


def _load_manifest() -> list[dict]:
    try:
        data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return []
    return data.get("beats", [])


def run() -> dict:
    started = time.time()
    results = []
    for entry in _load_manifest():
        name = entry.get("beat")
        fn = _beats.REGISTRY.get(name)
        if not fn:
            results.append({"id": name or "?", "label": name or "unknown",
                            "status": "warn", "detail": "no such beat",
                            "ms": 0, "extra": None})
            continue
        try:
            results.append(fn(entry.get("cfg") or {}))
        except Exception as e:
            results.append({"id": name, "label": name, "status": "alarm",
                            "detail": f"beat crashed: {type(e).__name__}: {e}",
                            "ms": 0, "extra": None})

    summary = {"ok": 0, "warn": 0, "alarm": 0}
    for r in results:
        summary[r["status"]] = summary.get(r["status"], 0) + 1

    worst = max((r["status"] for r in results),
                key=lambda s: STATUS_RANK.get(s, 0),
                default="ok")

    return {
        "generated":  datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "duration_ms": int((time.time() - started) * 1000),
        "worst":      worst,
        "summary":    summary,
        "beats":      results,
    }


def main():
    report = run()
    LOG_OUT.parent.mkdir(parents=True, exist_ok=True)
    LOG_OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
    bark_summary = _bark.sync(report["beats"])
    print(f"[spot] worst={report['worst']} "
          f"ok={report['summary']['ok']} warn={report['summary']['warn']} "
          f"alarm={report['summary']['alarm']}  ({report['duration_ms']}ms) "
          f"→ {LOG_OUT.relative_to(_REPO)}")
    print(f"[spot] bark: raised={bark_summary['raised']} "
          f"cleared={bark_summary['cleared']} kept={bark_summary['kept']}"
          + (f" skipped_no_db={bark_summary['skipped_no_db']}"
             if bark_summary.get("skipped_no_db") else ""))
    # Non-zero exit on alarm so CI/cron can notice.
    if report["worst"] == "alarm":
        sys.exit(2)


if __name__ == "__main__":
    main()
