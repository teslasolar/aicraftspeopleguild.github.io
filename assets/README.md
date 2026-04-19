# /assets — SVG atomic-design system

<div align="center"><img src="../guild/Enterprise/L2/hmi/web/assets/svg/widget-gallery.svg" alt="SvgOrganism gallery · the runtime/live equivalent of the static atoms here" width="1036"/></div>

> 🎨 **The static SVG design system.** Three-tier atomic design — atoms compose into molecules compose into organisms — mirroring the Python library at [`guild/Enterprise/L2/lib/svg_widget.py`](../guild/Enterprise/L2/lib/svg_widget.py) (which renders the *dynamic* live-data SVGs shown in the gallery above).
>
> Breadcrumb: [`/`](../) · `assets/`

## Layout

```
assets/svg/
├── atoms/       13 primitives  (dot · badge · divider · sparkline · chip · swatch · …)
├── molecules/    9 compositions (heartbeat-panel · kv-list · member-card · stat-card · status-row · alert-banner · …)
├── organisms/    5 full views   (dashboard · guild-banner · member-roster · …)
├── views/       layout wrappers + page shells
└── render.js    browser-side composer — stitches referenced atoms/molecules into an organism
```

## Two parallel systems

| this dir (`assets/svg/`) | `guild/Enterprise/L2/hmi/web/assets/svg/` |
|---|---|
| **Static** design-system — hand-authored SVGs | **Dynamic** — Python-generated, rebuilt on each heartbeat |
| Composed at runtime by `render.js` | Composed at build time by `svg_widget.py` + `build-*.py` |
| Used by the browser apps (terminal, whitepapers, phone apps) | Used by the README + SCADA dashboards |
| 13 atoms · 9 molecules · 5 organisms | 17 organisms in `tag.db.udts` |

Both systems share the same atom/molecule/organism vocabulary, and the same palette (`#0d1117`/`#161b22`/`#e6edf3`/`#3fb950`/…). You can port a primitive between them with minimal changes.

## See also

- [`guild/Enterprise/L2/lib/svg_widget.py`](../guild/Enterprise/L2/lib/svg_widget.py) — Python counterpart
- [`guild/Enterprise/L4/svg/`](../guild/Enterprise/L4/svg/) — dynamic generators
- [`guild/Enterprise/L4/svg/instances/organisms/`](../guild/Enterprise/L4/svg/instances/organisms/) — registry of the dynamic ones
