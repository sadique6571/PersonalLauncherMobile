# Personal Launcher Mobile

Independent Android phone launcher with the Personal Launcher theme family. The TV project, package, helpers and device settings remain separate.

## First development build

- Portrait Home and searchable all-apps grid, using recycled views and background app/icon loading.
- Saved Home favourites, four-app dock, add/remove and move-earlier/later controls. Long-press an app to edit it.
- TV Glass, Classic, Dune Loom, Pearl Current, Cobalt Rail, Sage Fold, Pine Mist and Amber Night.
- Per-theme intensity, transparency, font, accent, columns and icon corners; theme reset affects only that appearance.
- Local photo wallpaper via Android's document picker, decoded off the UI thread with a 2048px bound. Shared wallpaper and app ordering survive theme changes.
- White clock/date with shadow. Swipe up on the clock/date to open apps, swipe down to open the panel; explicit buttons are also available.
- Themed notification panel with real notifications, open/dismiss/clear-dismissible, app-provided actions and free-text replies where supported.
- Media volume control. Wi-Fi, Bluetooth and display buttons open the appropriate Android settings screens.
- Optional **Personal Panel** Quick Settings tile opens the panel while another app is active, without routing through Home.
- Supported Android default-Home role request. Android 16 system/keyboard/cutout insets keep controls clear of gestures.

## Setup and permissions

Open Launcher settings → Set as default Home, and approve Android's chooser. Notification access is optional: Launcher settings → Notification access. Notifications remain in memory; there is no INTERNET permission, telemetry, account login, notification-content logging or notification database.

To use the panel outside Home, edit Android's Quick Settings tiles and add **Personal Panel**. This tile opens our Activity; it does **not** replace the system notification shade or claim the top-edge gesture in other apps. Notification content is not shown while the phone is locked. Android may redact sensitive content, restrict actions or disconnect the listener. Work-profile/device policy restrictions remain in force.

No root, ADB helper or accessibility permission is required. The app does not disable Vivo Home, SystemUI, keyguard or permission controllers. Protected permission dialogs, biometric/PIN UI, Recents and the native shade remain Android/OriginOS features.

## Build in Android Studio

Open this repository's root directory. Use JDK 17, SDK platform 36 and Gradle 8.13 (AGP 8.13.2). Minimum Android version is 9 / API 28; target SDK is 36. Package: `com.sadique.launcher.mobile`, separate from the TV package. Display version stays **7.0**; internal versionCode is **2** for this independent app.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Windows: use `gradlew.bat` with the same tasks. GitHub Actions uploads **PersonalLauncherMobile-dev-N** under the workflow's Artifacts section. The ZIP contains `app-debug.apk`, unit-test results and lint report. The repository is private, so artifact download requires GitHub sign-in.

Development APKs use the runner's debug key and may have a different certificate on later builds. They are for first testing, not a production auto-update channel. Configure one persistent private release key before distributing upgradeable releases. Release assembly is checked but its unsigned APK is not offered as installable. No signing key is committed or generated into the source tree.

## Device validation still required

Intended first test device reports Android 16, OriginOS 6, 1080×2400, gesture navigation. Physical phone access is through the owner's Windows ADB session, not this build environment. CI cannot verify OriginOS Home/Back/Recents animations, notification delivery while idle, direct reply behavior, media-volume restrictions, image-provider availability or battery-management behavior. Test each on the real phone.

## Remaining mobile work

This is the first installable foundation, not feature parity with the TV launcher or OriginOS. Widgets, folders, drag-and-drop, additional Home pages, Daily Bing/weather, production signing and GitHub auto-updates remain to be implemented. Phone-specific shapes and glass rendering will be refined after on-device feedback; several themes currently share the same card geometry. Global replacement of Android system UI is not promised.

## Panel return-path regression

Tile launches use a temporary task with no Home affinity. Close removes only that task and reveals the app underneath. Opening the panel from Home stays inside Home and Close returns to Home. Android 16 emulator instrumentation checks both paths against real task IDs and window focus; the debug-only underlay activity is excluded from release APKs. Physical OriginOS retesting is still required.
