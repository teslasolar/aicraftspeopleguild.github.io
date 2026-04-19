# I'll Burn Toast and You Scrape It": A Hilariously Accurate Analysis of Software Development's Favorite Messy Divorce · Android

One-screen Compose app. Part of the ACG whitepaper library.

```bash
gradle assembleDebug                  # app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

Uses Gradle 8.1+, Kotlin 1.9, Compose BOM 2024.02.00. `applicationId`
is `com.aicraftspeopleguild.acg.papers.illburntoastandyouscrapeita` so this app coexists with its sibling papers
on one device.

Set `sdk.dir` in `local.properties` (Android Studio does this on
project open).
