# /guild — the ACG product root

<div align="center"><img src="Enterprise/L2/hmi/web/assets/svg/readme-hero.svg" alt="AI Craftspeople Guild · live ISA-95 control plane — this directory is the plant" width="1040"/></div>

> 🏛 **The product.** Everything the Guild site actually serves. Three parallel sub-trees — `apps/` (what a visitor *uses*), `Enterprise/` (the ISA-95 control plane — what a visitor *measures*), and `web/` (static shared assets).
>
> Breadcrumb: [`/`](../) · `guild/`

## Layout

```
guild/
├── apps/           browser-side experiences (visitor-facing)
│   ├── terminal/       ACG CLI in the browser + chat bridge
│   ├── whiteboard/     P2P collaborative whiteboard
│   ├── p2p/            raw mesh (WebRTC + WebTorrent tracker)
│   ├── whitepapers/    paper reader with offline cache
│   ├── paper-apps/     sub-apps embedded in papers
│   └── test/           in-browser smoke tests
│
├── Enterprise/     ISA-95 control plane (the "plant")
│   ├── L0/             physical process — human authoring, mobbing
│   ├── L1/             PLC — tag providers
│   ├── L2/             HMI runtime + SCADA + libs (scada/spot, scada/alarms, lib/svg_widget, …)
│   ├── L3/             build pipeline + UDT catalog
│   └── L4/             public API (JSON endpoints + SVG generators)
│
├── web/            shared web assets (css, icons, fonts)
├── guild/          legacy nested dir (kept for URL stability)
├── index.html      landing / NESW dock
└── path.json       dir manifest
```

## Two lenses on the same directory

```
┌─ a VISITOR sees ─────────────┐   ┌─ an OPERATOR sees ────────────┐
│                              │   │                               │
│  guild/apps/terminal/        │   │  guild/Enterprise/L2/scada/   │
│  guild/apps/whitepapers/     │   │  guild/Enterprise/L4/api/     │
│  guild/apps/p2p/             │   │  guild/Enterprise/L4/runtime/ │
│  guild/index.html            │   │  guild/Enterprise/L3/udts/    │
│                              │   │                               │
│  … chat, read, draw, mesh …  │   │  … tags, faults, patrols …   │
└──────────────────────────────┘   └───────────────────────────────┘
```

Both are first-class; neither is a leak of the other. The apps call the API endpoints; the API endpoints are populated by the SCADA layer; the SCADA layer watches the apps in turn via [SPOT](Enterprise/L2/scada/spot/README.md).

## See also

- [`Enterprise/README.md`](Enterprise/) — ISA-95 layer index + pyramid
- [`Enterprise/L2/scada/README.md`](Enterprise/L2/scada/) — SCADA subsystems (SPOT · alarms · gateway · programs · errors)
- [`Enterprise/L4/api/README.md`](Enterprise/L4/api/) — public JSON API surface
- [`apps/`](apps/) — visitor-facing browser apps
- [`../bin/`](../bin/) — `acg` CLI
- [`../README.md`](../) — root README (one-glance SCADA dashboard + SPOT + live widgets)
