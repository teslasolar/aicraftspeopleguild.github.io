# /guild/Enterprise/L2/scada

<div align="center"><img src="../hmi/web/assets/svg/scada-dashboard.svg" alt="ACG SCADA dashboard · eight framed panels · plant-state mega-lamp · SPOT patrol · live" width="1040"/></div>

> 🖥 **The SCADA layer.** Every subsystem below is a live HMI page. The board above aggregates them — clicking any panel takes you to that subsystem. Up: [`../`](../) (L2 HMI runtime). Breadcrumb: [`/`](../../../../) · [`/guild/Enterprise/`](../../) · [`/guild/Enterprise/L2/`](../) · `scada/`.

**Subsystems (each is its own live HMI page):**
- [`spot/`](spot/) 🐕 — SCADA patrol agent watching origin (security · integrity · manifesto tamper)
- [`alarms/`](alarms/) 🔔 — fault annunciator, reads `state.db.faults` live, filter + ack
- [`errors/`](errors/) ⚠ — error log
- [`gateway/`](gateway/) 🚪 — auth providers (Discord · GitHub · Google · WebRTC · WebTorrent)
- [`programs/`](programs/) 📋 — PLC program catalog

---

## ACGP2P · dense token spec

Declarative schema for every subsystem, in the compressed glyph style
from §0. Runtime modules in `/js/scada/`, `/js/*.js` and
`/controls/sandbox/*/modules/` mirror these files — diverge and the HMI
surfaces drift (manifest tag with no runtime entry → 🔴).

```
scada/
  00-legend.json   §0  decompressor · glyphs, arrows, type shorthand
  01-master.json   §1  🏗️ ACGP2P master UDT
  02-chat.json     §2  💬 Chat   (config.js · p2p.js · peers.js · chat.js)
  03-scada.json    §3  🖥️ SCADA  (scada/tags · providers · udt · monitor)
  04-sandbox.json  §4  🧪 Sandbox + 🧠 WebLLM
```

## Canonical shape

Every section JSON uses the same dense frame:

```jsonc
{
  "§":   <n>,
  "🏗️":  "<glyph>",
  "name": "<human name>",
  "module": ["source/path.js", …],

  "⚙️":  { /* static config · values in type shorthand */ },
  "📡|📦|…": { /* runtime state containers */ },
  "m<glyph>": "<key> → <shape>",

  "🏗️": {
    "<MsgName>": { "field": "type", … }
  },

  "→": [ { "name": "fn", "in": "sig", "flow": "a → b ⊕ c" }, … ],

  "↑🖥️": [ "<tag.path.*>", … ],
  "↓🖥️": [ "<tag.path.*>", … ],

  "和": true
}
```

Type shorthand (`s`, `s20`, `i`, `f`, `b`, `ms`, `o`, `a`, `m`, `e`, `?`)
and glyphs / arrows are defined once in `00-legend.json`.

## 🛰️ mesh envelope (§4)

```
{ source:s  type:s  path:s  value:any  ts:ms  … }
```

Channel: `BroadcastChannel("acg-mesh")`. Sandbox tools publish flat
envelopes; `js/sandbox-bridge.js` folds them into `sandbox.<source>.*`
in the main SCADA plant.

## Keeping runtime and manifest in sync

1. Edit the JSON here first.
2. Mirror in the matching JS (UDT in `…/scada/udt.js`; writes in the
   owning module).
3. Check the HMI — manifest entries with no runtime writes show as
   `quality:bad` or missing.
