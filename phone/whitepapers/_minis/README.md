# _minis · interactive layers

The papers are documentation. These modules are the running
implementations.

When a `WhitepaperApp` UDT instance at
`guild/Enterprise/L3/udts/whitepaper-app/instances/<slug>.json`
includes:

```json
"mini_app": "flywheel-tracker"
```

the generator (`phone/whitepapers/_generator/build.py`) first renders
the standard template into `phone/whitepapers/<slug>/android/`, then
OVERLAYS every file in `_minis/<mini_app>/android/` on top. Matching
relative paths overwrite the template stub; the per-paper app now
ships with a third bottom-nav tab ("Try") running the mini.

## Contract — `MiniRegistry`

Each mini must expose a top-level `object MiniRegistry` at
`.../papers/MiniRegistry.kt` with:

```kotlin
object MiniRegistry {
    fun isAvailable(): Boolean
    @Composable fun Render(modifier: Modifier, primary: Color)
}
```

`isAvailable()` flips the Mini tab on. `Render` gets a Modifier to
fill the tab pane and the paper's theme color for accent use.

## Current minis

| id                | paper     | what it does                          |
|-------------------|-----------|---------------------------------------|
| `flywheel-tracker`| flywheel  | cycle logger + streak counter         |

Add a new one by dropping
`_minis/<id>/android/app/src/main/java/com/aicraftspeopleguild/acg/papers/MiniRegistry.kt`,
then set `mini_app` on the paper's UDT instance and rerun `build.py`.
