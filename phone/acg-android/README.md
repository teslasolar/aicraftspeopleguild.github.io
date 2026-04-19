# ACG Android · Kotlin + Jetpack Compose

Native Android client for the [AI Craftspeople Guild](https://github.com/aicraftspeopleguild/aicraftspeopleguild.github.io)
control plane. Reads `/api/*.json` and the GitHub-Issues-backed tag DB
directly from the phone; writes tags through a PAT stored in
EncryptedSharedPreferences.

Fresh scaffold, v0.1.0 — no launcher yet, no background sync, no
offline cache. Four screens wired to the live API.

## Screens

| Tab         | What it does                                                 |
|-------------|--------------------------------------------------------------|
| Home        | `/api/health.json` + `/runtime/tags.json` enterprise snapshot |
| Tags        | Scrollable list of every open `label:tag` issue, filterable   |
| Heartbeat   | Read `demo.heartbeat` · **Bump** button (needs PAT)           |
| Settings    | Paste/clear GitHub PAT (EncryptedSharedPreferences)           |

## Building

Uses the Gradle you already have (8.1+). No wrapper committed — first
build generates it.

```bash
cd C:\Users\frumk\Desktop\ACG\acg-android
gradle wrapper                          # one-time: creates ./gradlew
./gradlew assembleDebug                 # builds app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

First run needs `local.properties` pointing at your Android SDK:

```
sdk.dir=C\:\\Users\\frumk\\AppData\\Local\\Android\\Sdk
```

(Android Studio creates this automatically on project open.)

## Layout

```
app/
├─ src/main/
│  ├─ AndroidManifest.xml
│  ├─ res/               themes, strings, adaptive launcher
│  └─ java/com/aicraftspeopleguild/acg/
│     ├─ MainActivity.kt         single-activity + bottom-nav
│     ├─ data/
│     │  ├─ Models.kt            Health · GhIssue · TagValue · CmdAction
│     │  ├─ ApiClient.kt         /api/*.json via OkHttp
│     │  ├─ GhTagClient.kt       list/read/write label:tag issues
│     │  └─ TokenStore.kt        EncryptedSharedPreferences
│     └─ ui/
│        ├─ AcgTheme.kt          dark Material3 palette (matches web)
│        ├─ HomeScreen.kt
│        ├─ TagBrowserScreen.kt
│        ├─ HeartbeatScreen.kt
│        └─ SettingsScreen.kt
```

## Dependencies (`gradle/libs.versions.toml`)

- AGP 8.7.2 · Kotlin 2.0.21 · compose-bom 2024.12.01 · Material3 1.3.1
- OkHttp 4.12 · kotlinx.serialization 1.7.3
- androidx.security.crypto 1.1.0-alpha06 (EncryptedSharedPreferences)

## Parity with the web stack

The Android app talks to the same endpoints as `guild/apps/control-deck/`
and `bin/acg-mcp.py`. Every feature the web UI exposes can be added
here in a future iteration:

- **whiteboard** — WebRTC via `libwebrtc` / `org.webrtc:google-webrtc`
- **p2p mesh** — WSS tracker client from plain Java/Kotlin
- **UDT editor** — a Compose Form over the same JSON schemas at
  `guild/Enterprise/L3/udts/<name>/template.json`
- **control-deck actions** — call `acg-mcp.cmd_action` via GitHub API
- **terminal** — reuse the REPL logic; keyboard → InputConnection

## Why native over a TWA

The TWA path would ship today but can't:
- store secrets outside a browser profile
- background-sync tags
- wake the device on a push
- render a proper Android share-sheet for paper links
- plug the phone's mic/camera into the whiteboard without a web shim

The Compose port starts thin (v0.1) and grows into those features as
they matter.
