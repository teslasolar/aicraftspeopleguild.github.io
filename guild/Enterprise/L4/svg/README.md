# /guild/Enterprise/L4/svg

<div align="center"><img src="../../L2/hmi/web/assets/svg/widget-gallery.svg" alt="Every SvgOrganism · 17 live widgets" width="1036"/></div>

> 🎨 **The SVG generators.** Each `build-*.py` reads live data (API endpoints · state.db · tag.db · external URLs) and emits one `.svg` into `guild/Enterprise/L2/hmi/web/assets/svg/`. Registered as `SvgOrganism` UDTs in [`instances/organisms/`](instances/organisms/) so [`svg_build_all`](../../L2/lib/svg_build_all.py) can iterate them on every heartbeat.
>
> Breadcrumb: [`/`](../../../../) · [`/guild/Enterprise/`](../../) · [`/guild/Enterprise/L4/`](../) · `svg/`

## Generators

| script | produces | reads |
|---|---|---|
| `build-readme-hero.py` | `readme-hero.svg` | tag bus, live counters |
| `build-architecture.py` | `architecture.svg` | ISA-95 layer manifest |
| `build-scada-dashboard.py` | `scada-dashboard.svg` | `health.json`, `state.json`, `spot-patrol.json`, tags, state.db |
| `build-spot-patrol.py` | `spot-patrol.svg` + `spot-patrol.json` | runs `spot.patrol` against origin + baselines |
| `build-fork-compare.py` | `fork-compare.svg` | `dual_source.pair()` against mine + origin |
| `build-heartbeat.py` | `heartbeat.svg` | `tag:demo.heartbeat` |
| `build-tag-grid.py` | `tag-grid.svg` | GitHub Issues API (label:tag) |
| `build-tag-activity.py` | `tag-activity.svg` | `state.db.tag_history` |
| `build-pipeline-pulse.py` | `pipeline-pulse.svg` | `state.db.pipeline_runs` |
| `build-status-dashboard.py` | `status-dashboard.svg` | same surfaces, condensed |
| `build-cmd-panel.py` | `cmd-panel.svg` | `cmd.yml` action catalog |
| `build-members-strip.py` | `members-strip.svg` | `members.json`, `papers.json` |
| `build-paper-roulette.py` | `paper-roulette.svg` | `papers.json` + heartbeat seed |
| `build-chat-llm-teaser.py` | `chat-llm-teaser.svg` | static teaser |
| `build-widget-gallery.py` | `widget-gallery.svg` | `tag.db.udts` (self-referential) |
| `build-readme-api.py` | `readme-api.svg` | live probe of every L4 endpoint |
| `build-readme-controls.py` | `readme-controls.svg` | static control nav |

## See also

- [`instances/organisms/`](instances/organisms/) — the JSON registry (how a generator becomes a discoverable widget)
- [`../../L2/lib/svg_widget.py`](../../L2/lib/svg_widget.py) — atom / molecule primitives (palette, pulse_dot, panel, chip, bar, sparkline, tag_card, stat_block…)
- [`../../L2/lib/svg_build_all.py`](../../L2/lib/svg_build_all.py) — the iterator
