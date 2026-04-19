# phone/whitepapers/ · one app per white paper

Two dimensions of output from one template:

- **Per-paper apps** — `phone/whitepapers/<slug>/{android,ios}/` — a
  focused single-screen app that ships just one paper. Great for
  independent Play Store / TestFlight listings.
- **Shared template** — `_template/android/` + `_template/ios/` — the
  only place code actually lives. Fix a bug once, `_generator/build.py`
  rolls it out to every paper on the next run.

```
phone/whitepapers/
├── _template/               single source of truth
│   ├── android/             Kotlin + Compose skeleton ({{PLACEHOLDERS}})
│   └── ios/                 SwiftUI skeleton (XcodeGen-based)
├── _generator/
│   └── build.py             renders every WhitepaperApp UDT instance
├── flywheel/                generated · pilot
│   ├── android/
│   └── ios/
├── <slug>/                  one dir per paper after `--all`
└── README.md                this file
```

## Regenerating

```bash
cd teslasolar
python phone/whitepapers/_generator/build.py --list        # show discovered papers
python phone/whitepapers/_generator/build.py --only flywheel
python phone/whitepapers/_generator/build.py               # render every instance
python phone/whitepapers/_generator/build.py --dry-run     # see the plan
```

Instances live at `guild/Enterprise/L3/udts/whitepaper-app/instances/<slug>.json`.
Add a JSON file there, rerun the generator, and a new paper app pops
out under its slug.

## Why a template?

Thirty papers × two platforms = sixty near-identical projects. Hand-
maintaining that is a full-time job. Templating pushes the per-paper
delta down to a ~30-line JSON file and lets the compiler + Apple's
review apparatus validate that each output is a legitimate, signable,
installable app.

## Per-paper vs umbrella

The per-paper apps are the show-off artefact — every paper gets its
own launcher icon, its own Play Store page, its own TestFlight build.
The **umbrella app** under `_umbrella/` (once built) ships every paper
in one install for the reader who wants the library rather than the
shelf. Same UDT instances feed both.

## Adding a new paper

1. Create `L3/udts/whitepaper-app/instances/<slug>.json` with `slug`,
   `title`, `author`, `date`, `doc_number`, `abstract`, `paper_url`,
   `android_app_id`, `ios_bundle_id`, `theme_color_hex`, optional `label`.
2. `python phone/whitepapers/_generator/build.py --only <slug>`.
3. Android: `cd phone/whitepapers/<slug>/android && gradle assembleDebug`.
4. iOS: `cd phone/whitepapers/<slug>/ios && xcodegen generate && open *.xcodeproj`.
