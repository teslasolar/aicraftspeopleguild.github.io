"""spot.bark — translate patrol results into state_db faults so the
existing alarm annunciator HMI (scada/alarms) picks them up without
knowing anything about SPOT.

Mapping per beat:
  status ok     → clear any active fault for that beat's tag
  status warn   → raise_fault(severity="warn",  tag=scada.spot.<id>)
  status alarm  → raise_fault(severity="error", tag=scada.spot.<id>)

Tag convention keeps SPOT-originated faults grouped under the
`scada.spot.*` prefix so operators can filter.
"""
from __future__ import annotations

import sys
from pathlib import Path

_REPO = Path(__file__).resolve().parents[5]
sys.path.insert(0, str(_REPO / "guild" / "Enterprise" / "L2" / "lib"))

try:
    import state_db  # type: ignore
except Exception:   # state DB optional in ephemeral CI environments
    state_db = None

_SEV = {"alarm": "error", "warn": "warn"}


def _tag(beat_id: str) -> str:
    return f"scada.spot.{beat_id}"


def sync(results: list[dict]) -> dict:
    """Reconcile patrol results with state_db. Returns a small summary
    so the caller can log it. Safe to call if state_db is unavailable —
    it becomes a no-op."""
    summary = {"raised": 0, "cleared": 0, "kept": 0, "skipped_no_db": 0}
    if state_db is None:
        summary["skipped_no_db"] = len(results)
        return summary

    # Single DB read, then map by tag — avoids N queries per sweep.
    active = state_db.list_faults(active_only="1").get("faults") or []
    by_tag: dict[str, dict] = {f["tag"]: f for f in active if f.get("tag")}

    # Orphan sweep: clear any scada.spot.* fault whose beat wasn't in
    # this run. Prevents retired beats (renamed / deleted) from leaving
    # permanent red cards in the alarms HMI.
    current_tags = {_tag(r.get("id") or "unknown") for r in results}
    for tag, f in list(by_tag.items()):
        if tag.startswith("scada.spot.") and tag not in current_tags:
            state_db.clear_fault(tag=tag)
            summary["cleared"] += 1
            del by_tag[tag]

    for r in results:
        tag = _tag(r.get("id") or "unknown")
        status = r.get("status", "ok")
        prior = by_tag.get(tag)

        if status == "ok":
            if prior:
                state_db.clear_fault(tag=tag)
                summary["cleared"] += 1
            continue

        desired_sev = _SEV.get(status, "warn")
        if prior and (prior.get("severity") == desired_sev
                      and prior.get("message") == r.get("detail", "")):
            summary["kept"] += 1
            continue

        # Severity or message changed → clear prior, raise fresh so
        # operators see the new state without losing fault_id history.
        if prior:
            state_db.clear_fault(tag=tag)
        state_db.raise_fault(
            kind="spot-patrol",
            tag=tag,
            severity=desired_sev,
            message=r.get("detail", "")[:240],
            detail={"beat": r.get("id"), "label": r.get("label"),
                    "ms": r.get("ms"), "extra": r.get("extra")},
        )
        summary["raised"] += 1
    return summary
