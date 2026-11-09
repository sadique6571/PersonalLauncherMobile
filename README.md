# Personal Launcher Mobile

Independent Android phone launcher with the Personal Launcher theme family. The TV project, package, helpers and device settings remain separate.

## First development build

- Portrait Home and searchable all-apps grid, using recycled views and background app/icon loading.
- Saved Home favourites, four-app dock, add/remove and move-earlier/later controls. Long-press an app to edit it.
- TV Glass, Classic, Dune Loom, Pearl Current, Cobalt Rail, Sage Fold, Pine Mist, Amber Night and iPhone Glass.
- Per-theme intensity, transparency, font, accent, columns and icon corners; theme reset affects only that appearance.
- Local photo wallpaper via Android's document picker, decoded off the UI thread with a 2048px bound. Shared wallpaper and app ordering survive theme changes.
- White clock/date with shadow. Swipe up on Home to open apps, swipe down to open the panel; child scrolling is preserved when the grid can scroll. Explicit icon buttons are also available.
- Built-in Home widget: a glass card with the live clock, date and live weather. Weather comes from Open-Meteo (free, no key) for the phone's current coarse location; it needs INTERNET and the user-approved ACCESS_COARSE_LOCATION permission (coarse only — no fine location, no continuous or background tracking, coordinates are never stored and go only to Open-Meteo for that one request). If location is denied, off or unavailable, the widget falls back to Okara (30.81, 73.45) and says so on the widget; offline it shows the last cached reading, or no weather line at all. No weather data is logged or uploaded anywhere else.
- App Library: inside the drawer, "App Library" groups every app by the first letter of its label (A–Z sections, then "#"). Android exposes no reliable per-app category, so this is a label-initial grouping, not a store category. Search ("Search apps") filters the same apps either way.
- Control Center: the Personal Panel now has a larger time header (56sp) with a "Control Center" subtitle, fully rounded 28dp toggle cards (Wi-Fi, Bluetooth, Display, Swipe access) and rounded 20dp notification cards; the full-height vertical brightness/volume capsules are unchanged. It still does **not** replace the Android system shade — see "Setup and permissions".
- Themed notification panel with real notifications, open/dismiss/clear-dismissible, app-provided actions and free-text replies where supported.
- Full-height brightness and volume capsules. System brightness requires optional user-approved Modify system settings access; dragging brightness selects manual brightness. Wi-Fi, Bluetooth and display buttons open the appropriate Android settings screens.
- Optional **Personal Panel** Quick Settings tile opens the panel while another app is active, without routing through Home.
- Supported Android default-Home role request. Android 16 system/keyboard/cutout insets keep controls clear of gestures.

## Setup and permissions

Open Launcher settings → Set as default Home, and approve Android's chooser. Notification access is optional: Launcher settings → Notification access. Notifications remain in memory; there is no telemetry, account login, notification-content logging or notification database. INTERNET is used only for the Home widget weather (Open-Meteo, current coarse location or the Okara fallback — see above); nothing else uses the network. Location access is optional and coarse-only: the first time the widget shows, Android asks once; deny it and the widget falls back to Okara. Grant it later from Launcher settings → Weather location, which opens the app's Android settings page. No location is tracked, stored or shared.

To use the panel outside Home, edit Android's Quick Settings tiles and add **Personal Panel**. This tile opens our Activity; it does **not** replace the system notification shade or claim the top-edge gesture in other apps. Notification content is not shown while the phone is locked. Android may redact sensitive content, restrict actions or disconnect the listener. Work-profile/device policy restrictions remain in force.

No root or ADB helper is required. Core Home, tile and notifications do not require accessibility access. Optional **Personal Panel edge swipe** uses a user-enabled AccessibilityService to draw a small touch handle just below the top-right status bar over apps; it does not retrieve window content, inspect text, filter keys or trigger actions without a handle gesture. Android’s reserved top-edge gesture retains access to its native shade; the visible handle is an alternate entry gesture. Disable the service in launcher settings or Android Accessibility settings. OriginOS behavior must be tested on the phone. The app does not disable Vivo Home, SystemUI, keyguard or permission controllers. Protected permission dialogs, biometric/PIN UI, Recents and the native shade remain Android/OriginOS features.

## Build in Android Studio

Open this repository's root directory. Use JDK 17, SDK platform 36 and Gradle 8.13 (AGP 8.13.2). Minimum Android version is 9 / API 28; target SDK is 36. Package: `com.sadique.launcher.mobile`, separate from the TV package. Display version stays **7.0**; internal versionCode is **4** for this independent app.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Windows: use `gradlew.bat` with the same tasks. GitHub Actions uploads **PersonalLauncherMobile-dev-N** under the workflow's Artifacts section. The ZIP contains `app-debug.apk`, unit-test results and lint report. The repository is private, so artifact download requires GitHub sign-in.

Development APKs use the runner's debug key and may have a different certificate on later builds. They are for first testing, not a production auto-update channel. Configure one persistent private release key before distributing upgradeable releases; see [SIGNING.md](SIGNING.md). Once configured, successful main builds publish a signed APK in GitHub Releases. Release assembly is checked but its unsigned APK is not offered as installable. No signing key is committed or generated into the source tree.

## Device validation still required

Intended first test device reports Android 16, OriginOS 6, 1080×2400, gesture navigation. Physical phone access is through the owner's Windows ADB session, not this build environment. CI cannot verify OriginOS Home/Back/Recents animations, notification delivery while idle, direct reply behavior, media-volume restrictions, image-provider availability or battery-management behavior. Test each on the real phone.

## Remaining mobile work

This is the first installable foundation, not feature parity with the TV launcher or OriginOS. The built-in Home widget (clock/date/weather) and the label-initial App Library are done; folders, drag-and-drop, additional Home pages, Daily Bing, production key configuration and in-app GitHub auto-updates remain to be implemented. Hosting other apps' widgets (AppWidgetHost) is future work — the Home widget is built-in only. Phone-specific shapes and glass rendering will be refined after on-device feedback; several themes currently share the same card geometry. Global replacement of Android system UI is not promised.

## Panel return-path regression

Tile launches use a temporary task with no Home affinity. Close removes only that task and reveals the app underneath. Opening the panel from Home stays inside Home and Close returns to Home. Android 16 emulator instrumentation checks both paths against real task IDs and window focus; the debug-only underlay activity is excluded from release APKs. Physical OriginOS retesting is still required.

## iPhone Glass theme

The ninth theme, **iPhone Glass** (dark navy gradient, iOS system-blue accent), restyles the existing Home and panel without adding new screens:

- Squircle app-icon plates: the icon corner is capped at 18dp on the 58dp icon, so icons stay squircle even when the panel corner radius is raised. Other themes use the panel radius for icons, unchanged.
- Glass pill dock: the 4-app dock is a full pill (44dp corner radius) with the theme glass bevel. Other themes keep their panel-radius dock, unchanged.
- Control Center style panel: the Personal Panel keeps its rounded toggle cards (Wi-Fi, Bluetooth, Display, Swipe access) and its full-height vertical brightness/volume capsules; only the palette and corner geometry change with the theme.
- Home clock and date stay white with shadow, exactly as in the other themes.

What iPhone Glass does **not** do: it is a Home/panel theme, not an iPhone. There is no lock screen and no Face ID unlock — an Android launcher cannot replace the lock screen; Android owns the keyguard, PIN/biometric UI, Recents and the system shade. Calculator, Messages, Photos and other apps are not built into the launcher; tapping their icons opens the phone's own apps, with their own Android icons. App icons keep their Android artwork; only the squircle plate behind them is themed.

## Home widget, App Library and Control Center

What this build adds, and what it deliberately does not:

- Home widget: built-in only — live clock, date and Open-Meteo weather for the current coarse location, Okara fallback when location is unavailable. No live weather without INTERNET; no fine location; no tracking; no stored coordinates.
- App Library: label-initial grouping (A–Z, then "#"). It is not a Play Store category view — Android gives the launcher no reliable per-app category.
- Control Center: rounder cards and a larger header on the same Personal Panel. It opens the same Android settings screens and the same vertical sliders. It does not replace the system shade, the lock screen or the keyguard.

## Mobile control-center styling

Icon-only Home toolbar, icon dock, theme-owned glass bevels, compact notification action rows and full-height level capsules replace the initial text-button prototype. Where Android reports cross-window blur support, the panel blurs the app behind it; otherwise it uses a readable solid palette. Android remains responsible for the system shade and protected dialogs. The accessibility handle is an optional alternate entry gesture, not a replacement SystemUI implementation.

## Restoring the initial test preferences

After retaining an explicit XML backup from the debug test installation, use Launcher settings → Import saved settings (.xml). Only whitelisted Home/dock, wallpaper URI, selected style and per-theme appearance values are imported; permissions, notification data and signing material are excluded. The file is size-bounded and parsed before preferences are changed. Android-controlled permissions and persisted photo access must be granted separately after a signature-change reinstall.
