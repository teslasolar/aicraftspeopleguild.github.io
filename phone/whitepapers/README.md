# phone/whitepapers/ · one app per white paper

Every whitepaper on the guild site ships as its own phone app.
Each app has three tabs:

| tab     | what it shows                                                          |
|---------|------------------------------------------------------------------------|
| Paper   | title / author / date / abstract · offline-safe hero                   |
| Read    | WebView of the paper itself (body_url) — the full document             |
| Try     | interactive pocket implementation of the paper's idea                  |

The paper is the documentation. The **Try** tab is the running code.

## The 25

| slug                                        | title                                                             | try                                  |
|---------------------------------------------|-------------------------------------------------------------------|--------------------------------------|
| flywheel                                    | The Flywheel                                                      | cycle logger + streak counter        |
| you-re-absolutely-wrong                     | "You're Absolutely Wrong!"                                        | certainty reversal log               |
| ai-that-pisses-you-off-the-surprisingly     | AI That Pisses You Off                                            | pushback dial 0 → 100                |
| acg-kcc-guild-chain                         | ACG-KCC: Guild Chain                                              | guild-coin ledger                    |
| acg-net-decentralized-guild-web             | ACG-NET: Decentralized Guild Web                                  | live mesh visualizer                 |
| lightning-factory                           | Lightning Factory                                                 | 7.83 Hz Schumann pulse               |
| the-dog-the-data-scientist-and-the-mrna     | The Dog, the Data Scientist, and the mRNA Vaccine                 | Bayes pocketbook                     |
| the-harm-equation                           | The Harm Equation                                                 | severity × reach × (1 − reversibility) |
| the-konomi-standard                         | The Konomi Standard                                               | UDT JSON validator                   |
| the-prediction-trap                         | The Prediction Trap                                               | calibration logger                   |
| ass-os-bloom-prompt-the-irrational          | 🌸 ASS-OS BLOOM — Irrational Universe                              | irrational prompt deck               |
| ass-os-bloom-prompt-the-rational-empire     | 🌸 ASS-OS BLOOM — Rational Empire                                  | rational prompt deck                 |
| the-shield-of-all-knights-against           | The Shield of All Knights Against Bounded Rationality             | bias flashcards                      |
| from-correctness-to-integrity-measuring     | From Correctness to Integrity                                     | code-integrity inspector             |
| avoid-being-fractally-wrong                 | Avoid Being Fractally Wrong                                       | Sierpinski depth demo                |
| the-pattern-that-wasn-t-there               | The Pattern That Wasn't There                                     | pareidolia game                      |
| ai-harness-constraint-driven-software       | AI Harness: Constraint-Driven Software Development                | constraint ledger                    |
| convergent-governance-topologies-in         | Convergent Governance Topologies in Specialist Networks           | governance-form classifier           |
| toastmasters-scrapers-guild                 | Toastmasters Scrapers Guild                                       | URL auditor                          |
| acg-review-forge                            | ACG Review Forge                                                  | copy-to-clipboard prompts            |
| grid-brain                                  | Grid Brain                                                        | Game of Life on an 18×18             |
| s-a-d                                       | S.A.D.                                                            | 30-day mood sparkline                |
| structural-toast-carbonization-a-failure    | Structural Toast Carbonization                                    | toast-ageing clock                   |
| i-ll-burn-toast-and-you-scrape-it-a         | "I'll Burn Toast and You Scrape It"                               | burner / scraper quiz                |
| ass-os-bloom-prompt-occam                   | 🌸 ASS-OS BLOOM — OCCAM                                            | verbosity filter                     |

## Layout

```
phone/whitepapers/
├── _template/                      Kotlin + Compose + SwiftUI skeleton
├── _minis/<slug>/                  per-paper interactive overlay
├── _generator/
│   ├── build.py                    renders template + overlays minis
│   ├── pull-instances.py           re-seeds UDT instances from papers.json
│   └── build-umbrella.py           aggregates papers.json for the umbrella
├── _umbrella/                      one install for the whole library
└── <slug>/                         generated per-paper apps (Android + iOS)
```

## Regenerating

```bash
# pull latest paper metadata from the fork's Pages host
python phone/whitepapers/_generator/pull-instances.py --force

# regenerate every per-paper app (+ overlays each mini)
python phone/whitepapers/_generator/build.py

# refresh the umbrella's bundled papers.json
python phone/whitepapers/_generator/build-umbrella.py

# build one APK end-to-end
cd phone/whitepapers/flywheel/android && gradle assembleDebug
```

## Writing a new mini

1. Create `phone/whitepapers/_minis/<slug>/android/app/src/main/java/com/aicraftspeopleguild/acg/papers/MiniRegistry.kt`.
2. Implement:

    ```kotlin
    package {{ANDROID_PACKAGE}}
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color

    object MiniRegistry {
        fun isAvailable(): Boolean = true
        @Composable fun Render(modifier: Modifier, primary: Color) { /* your code */ }
    }
    ```
3. On the UDT instance, set `"mini_app": "<slug>"`.
4. Rerun `build.py --only <slug>`.

The stub at `_template/.../MiniRegistry.kt` hides the Try tab when no overlay is present, so the contract is safe to break in progress — unwired papers stay 2-tab.

## iOS parity

iOS per-paper apps ship today with just Paper + Read (WebView). Minis
will land on iOS in SwiftUI in a follow-up; the bundle ids + TestFlight
matrix workflow are already wired so all 25 can upload as soon as the
Swift side catches up.

## TestFlight

See `.github/workflows/ios-testflight-matrix.yml` and
`phone/acg-ios/README.md` for the one-time Apple Developer Program
enrollment + ASC secret setup. Once those are in, every push to
`feat/mirror-compare` archives + uploads all 25 paper builds plus the
umbrella in one run.
