# flywheel-tracker

Mini interactive layer for the **Flywheel** whitepaper.

The paper argues compounding returns come from a tight loop — write,
ship, review, teach, write — and that that loop is the only one that
grows without burning operators out. This module is the minimum
implementation of it on the phone:

- **+ log a cycle** — one tap per trip around the loop
- **total** — lifetime cycles
- **streak** — consecutive days with at least one cycle (today or yesterday counts as "kept it going")
- **today** — cycles logged in the current local day
- **recent** — the last 40 timestamps

Persistence: `SharedPreferences("flywheel-mini")` under key `cycles`,
comma-separated epoch-ms. Zero deps, survives uninstall-only.

## Overlay semantics

The generator at `phone/whitepapers/_generator/build.py` copies every
file under `_minis/<id>/android/` over the rendered template dir when
a `WhitepaperApp` UDT instance has `mini_app: "<id>"`. Files match by
relative path — this `MiniRegistry.kt` overrides the template stub
at `app/src/main/java/com/aicraftspeopleguild/acg/papers/MiniRegistry.kt`.

Your mini must define an object with this shape (see the stub in
`_template/android/…/MiniRegistry.kt`):

```kotlin
object MiniRegistry {
    fun isAvailable(): Boolean
    @Composable fun Render(modifier: Modifier, primary: Color)
}
```

`{{ANDROID_PACKAGE}}` placeholder in source files gets substituted by
the generator, same as the rest of the template.
