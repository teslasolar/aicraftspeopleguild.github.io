# _umbrella · ACG Papers

One installable app that bundles every whitepaper. Sibling to the
per-paper apps — they ship separately for the "here's a paper, here's
an app" moment, this one ships for the "give me the library"
audience.

```
_umbrella/
├── android/                    Compose LazyColumn + hero detail
├── ios/                        SwiftUI List + NavigationStack detail
└── <each half reads papers.json bundled in assets / Resources>
```

Refresh the bundled library whenever paper instances change:

```bash
python phone/whitepapers/_generator/build-umbrella.py
```

Both halves render the same shape as a per-paper app once you tap
through — same hero block, same "Read the full paper ↗" link-out —
but never need to be regenerated per paper.

## Bundle ids

- `com.aicraftspeopleguild.acg.papers` — the umbrella
- `com.aicraftspeopleguild.acg.papers.<slug>` — per-paper apps

They coexist on one device; installing the umbrella doesn't conflict
with any of the individual apps.
