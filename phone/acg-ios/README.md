# ACG iOS · SwiftUI

Native iPhone/iPad client, mirrors the `phone/acg-android` app.

## Layout

```
ACG/
├── ACGApp.swift                entry point, wires the World singleton
├── ContentView.swift           5-tab TabView (Home / Tags / Heart / Plant / Set)
├── Data/
│   ├── ApiClient.swift         URLSession wrapper over the public L4 API
│   ├── GhTagClient.swift       list / read / write label:tag issues
│   ├── TagDb.swift             JSON-file-backed tag cache (swap for GRDB later)
│   ├── SyncEngine.swift        GH → TagDb + Mqtt fanout
│   ├── Mqtt.swift              topic pub/sub with * / # wildcards
│   ├── SensorPlant.swift       CoreMotion-backed sensor plant
│   └── TokenStore.swift        Keychain-backed PAT
├── Screens/                    5 @Composable-equivalent Views
├── UDT/UdtRegistry.swift       loads Resources/udt/*.json at startup
└── Resources/udt/              duplicate of acg-android/app/src/main/assets/udt/
```

The UDT JSON catalog is shared semantics between the two apps —
both read the same instance files. The iOS copy is duplicated for
now; a sync script will unify them once the layout settles.

## Build

You'll need macOS + Xcode 15 (iOS 17 deployment target).

```bash
brew install xcodegen
cd teslasolar/phone/acg-ios
xcodegen generate     # writes ACG.xcodeproj from project.yml
open ACG.xcodeproj
```

No third-party packages — everything ships in the Swift + iOS SDK.

## Installing on a real device

iOS doesn't allow sideloading an unsigned IPA. Two paths:

### (a) Personal-team dev build · free · fastest for one tester

Your buddy opens `ACG.xcodeproj` in Xcode, selects his own Apple ID
under *Signing & Capabilities → Team*, plugs in his iPhone, and hits
▶. App runs for 7 days before needing re-signing. No Apple Developer
Program needed, but he has to do it on his own Mac.

### (b) TestFlight · $99/yr Apple Developer Program · multiple testers

One-time setup on the fork owner side:

1. Enroll in the Apple Developer Program at
   https://developer.apple.com/programs/ (~$99 / yr).
2. Create an App Store Connect app record with bundle id
   `com.aicraftspeopleguild.acg` at
   https://appstoreconnect.apple.com.
3. Generate an ASC API key (Users & Access → Keys → +). Download the
   `.p8` file, note the Key ID + Issuer ID.
4. Add four secrets on the teslasolar GitHub fork
   (Settings → Secrets → Actions):
   - `APP_STORE_CONNECT_API_KEY_ID`
   - `APP_STORE_CONNECT_API_ISSUER_ID`
   - `APP_STORE_CONNECT_API_KEY_P8`  (paste the whole .p8 text)
   - `APPLE_TEAM_ID`                 (10-char team id from the same page)

Then every push touching `phone/acg-ios/**` runs
`.github/workflows/ios-testflight.yml`, which:

- runs `xcodegen generate`
- `xcodebuild archive`
- exports an `.ipa`
- uploads to TestFlight via `altool`

Add your buddy as an **internal tester** in ASC — he gets an invite
email within ~30 min of Apple processing the build, no review needed.
External tester slots require Apple's ~24 h review the first time only.

## First run

- Default host: `https://teslasolar.github.io/aicraftspeopleguild.github.io`
- GH-Issues tag DB: `teslasolar/aicraftspeopleguild.github.io`
- Home loads catalog + enterprise snapshot on appear.
- Plant starts CoreMotion on appear, stops on disappear.
- Heartbeat's **Bump** needs a PAT entered on Settings.
- Settings → UDT Catalog shows every instance loaded from the bundle.

## Parity with Android (current)

| feature             | Android | iOS |
|---------------------|---------|-----|
| 5-tab bottom nav    | ✓       | ✓   |
| /api/* snapshot     | ✓       | ✓   |
| GH-Issues tag DB    | ✓       | ✓   |
| Local tag cache     | SQLite  | JSON (swap to GRDB later) |
| Mqtt bus            | ✓       | ✓   |
| Sensor plant        | SensorManager | CoreMotion |
| UDT registry + Settings catalog | ✓ | ✓ |
| PAT storage         | EncryptedSharedPreferences | Keychain |
| TagEventScript runtime | pending | pending |
| Actuators (torch/vibrate/notify) | declared | declared |

## Known gaps

- Bundle signing: not configured. Buddy will need to set a Development Team in Xcode before running on a device.
- No push notification entitlement yet — `actuator:notify` will use local `UNUserNotificationCenter` once the runtime lands.
- iOS doesn't expose a `light` sensor via public APIs — the UDT catalog declares it but SensorPlant won't emit readings. Ambient brightness via `UIScreen.main.brightness` could stand in.
