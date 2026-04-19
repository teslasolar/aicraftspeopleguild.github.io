# phone/ · mobile clients for the ACG control plane

Two native apps, one shared semantic vocabulary.

```
phone/
├── acg-android/    Kotlin + Jetpack Compose  (gradle 8.1 · Kotlin 1.9 · AGP 8.1)
├── acg-ios/        SwiftUI                   (XcodeGen · iOS 17)
└── README.md       you are here
```

Both apps:

- hit the same **L4 REST API** (`/api/health.json`, `/runtime/tags.json`)
- read/write the same **GitHub-Issues tag DB** (`teslasolar/aicraftspeopleguild.github.io`)
- ship the same **UDT asset catalog** — `assets/udt/` on Android,
  `Resources/udt/` on iOS — so scripts/screens/actuators are described once
- expose a 5-tab shell with identical labels (Home · Tags · Heart · Plant · Set)

## Why both?

Android first because I had the device. iOS because a collaborator
has an iPhone and wants to test. The shared tag-DB and UDT catalog
means whatever one app does (publishes a sensor reading, bumps a
heartbeat, fires an actuator) shows up in the other via the same
`tag:<path>` issue on the org / fork repo.

## Installing on a device

- **Android**: `./gradlew :app:installDebug` with the phone in
  wireless-debug pairing mode. See `acg-android/README.md`.
- **iOS**: requires signing. Either the buddy opens the Xcode project
  with his own Apple ID (7-day free personal-team build) OR go
  through the paid Developer Program + the `ios-testflight` workflow.
  See `acg-ios/README.md` for the TestFlight recipe.

## Keeping the two in sync

The UDT JSON files are currently duplicated in both apps. When we
start iterating quickly, run:

```bash
# from teslasolar/
rsync -av --delete \
  phone/acg-android/app/src/main/assets/udt/ \
  phone/acg-ios/ACG/Resources/udt/
```

A dedicated `bin/sync-udt-assets.py` will follow. Long term, a
single top-level `phone/udt/` and both builds pulling from there via
their respective asset-inclusion rules.
