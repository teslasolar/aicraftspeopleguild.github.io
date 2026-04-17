#!/usr/bin/env bash
# ACG site build pipeline
# Orchestrates all content pipelines; produces guild/web/dist/
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
cd "$REPO"

echo "[build] 1/7 Extract components from engineering docs"
python guild/web/scripts/components/extract.py

echo ""
echo "[build] 2/7 Build component UDT/tag catalog"
python guild/web/scripts/components/build-catalog.py

echo ""
echo "[build] 3/7 Rebuild white paper UDT instances"
cd guild/web/white-papers && python ingest.py && cd "$REPO"

echo ""
echo "[build] 4/7 Generate white paper apps (one App per paper)"
python guild/web/scripts/apps/build-whitepaper-apps.py

echo ""
echo "[build] 5/7 Render view trees to dist/"
node guild/web/scripts/build.js

echo ""
echo "[build] 6/7 Overlay white-papers + members index from UDT instances"
python guild/web/scripts/white-papers/regen-index.py
python guild/web/scripts/members/regen-index.py

echo ""
echo "[build] 7/7 Done. dist/ contains $(ls guild/web/dist/*.html 2>/dev/null | wc -l) pages."
