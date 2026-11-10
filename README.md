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
- Home-screen widget pack: **10 home-screen widgets** (Analog Clock, Analog Clock Dark, Analog Clock + Weather, Digital Clock + Weather, Digital Clock, Weather, Analog Clock + Date, Big Clock + Weather, Mechanical Clock, Clock + Weather) — real Android home-screen widgets, so they work on the phone's default launcher (Vivo Home / OriginOS) and this app does not need to be the default Home. Glass cards with real analog clocks (our own dial and hands, hour/minute plus an orange second hand), big digital digits, the date and detailed Open-Meteo weather (feels-like, high/low, humidity, wind, rain chance, sunrise/sunset). Tap a weather widget to open the full Weather screen; tap a clock widget to open the app; see "Home-screen widget pack" below.
- App Library: inside the drawer, "App Library" groups every app by the first letter of its label (A–Z sections, then "#"). Android exposes no reliable per-app category, so this is a label-initial grouping, not a store category. Search ("Search apps") filters the same apps either way.
- Control Center: the Personal Panel now has a larger time header (56sp) with a "Control Center" subtitle, fully rounded 28dp toggle cards (Wi-Fi, Bluetooth, Display, Swipe access) and rounded 20dp notification cards; the full-height vertical brightness/volume capsules are unchanged. It still does **not** replace the Android system shade — see "Setup and permissions".
- Themed notification panel with real notifications, open/dismiss/clear-dismissible, app-provided actions and free-text replies where supported.
- Full-height brightness and volume capsules. System brightness requires optional user-approved Modify system settings access; dragging brightness selects manual brightness. Wi-Fi, Bluetooth and display buttons open the appropriate Android settings screens.
- Optional **Personal Panel** Quick Settings tile opens the panel while another app is active, without routing through Home.
- Supported Android default-Home role request. Android 16 system/keyboard/cutout insets keep controls clear of gestures.

## Setup and permissions

Open Launcher settings → Set as default Home, and approve Android's chooser. Notification access is optional: Launcher settings → Notification access. Notifications remain in memory; there is no telemetry, account login, notification-content logging or notification database. INTERNET is used only for the Home widget weather (Open-Meteo, current coarse location or the Okara fallback — see above); nothing else uses the network. Location access is optional and coarse-only: the first time the widget shows, Android asks once; deny it and the widget falls back to Okara. Grant it later from Launcher settings → Weather location, which opens the app's Android settings page. No location is tracked, stored or shared. Grant it later from Launcher settings → Weather location, which opens the app's Android settings page. No location is tracked, stored or shared. The same permission feeds the home-screen widgets: add a widget, tap it once to open the app, and allow location when Android asks — the widget then shows the current location's weather. A widget cannot ask for a runtime permission itself, so this one tap is the only step.

To use the panel outside Home, edit Android's Quick Settings tiles and add **Personal Panel**. This tile opens our Activity; it does **not** replace the system notification shade or claim the top-edge gesture in other apps. Notification content is not shown while the phone is locked. Android may redact sensitive content, restrict actions or disconnect the listener. Work-profile/device policy restrictions remain in force.

No root or ADB helper is required. Core Home, tile and notifications do not require accessibility access. Optional **Personal Panel edge swipe** uses a user-enabled AccessibilityService to draw a small touch handle just below the top-right status bar over apps; it does not retrieve window content, inspect text, filter keys or trigger actions without a handle gesture. Android’s reserved top-edge gesture retains access to its native shade; the visible handle is an alternate entry gesture. Disable the service in launcher settings or Android Accessibility settings. OriginOS behavior must be tested on the phone. The app does not disable Vivo Home, SystemUI, keyguard or permission controllers. Protected permission dialogs, biometric/PIN UI, Recents and the native shade remain Android/OriginOS features.

## Build in Android Studio

Open this repository's root directory. Use JDK 17, SDK platform 36 and Gradle 8.13 (AGP 8.13.2). Minimum Android version is 9 / API 28; target SDK is 36. Package: `com.sadique.launcher.mobile`, separate from the TV package. Display version stays **7.0**; internal versionCode is **5** for this independent app.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Windows: use `gradlew.bat` with the same tasks. GitHub Actions uploads **PersonalLauncherMobile-dev-N** under the workflow's Artifacts section. The ZIP contains `app-debug.apk`, unit-test results and lint report. The repository is private, so artifact download requires GitHub sign-in.

Development APKs use the runner's debug key and may have a different certificate on later builds. They are for first testing, not a production auto-update channel. Configure one persistent private release key before distributing upgradeable releases; see [SIGNING.md](SIGNING.md). Once configured, successful main builds publish a signed APK in GitHub Releases. Release assembly is checked but its unsigned APK is not offered as installable. No signing key is committed or generated into the source tree.

## Device validation still required

Intended first test device reports Android 16, OriginOS 6, 1080×2400, gesture navigation. Physical phone access is through the owner's Windows ADB session, not this build environment. CI cannot verify OriginOS Home/Back/Recents animations, notification delivery while idle, direct reply behavior, media-volume restrictions, image-provider availability or battery-management behavior. Test each on the real phone.

## Remaining mobile work

This is the first installable foundation, not feature parity with the TV launcher or OriginOS. The built-in Home widget (clock/date/weather), the home-screen widget pack (10 widgets) and the label-initial App Library are done; folders, drag-and-drop, additional Home pages, Daily Bing, production key configuration and in-app GitHub auto-updates remain to be implemented. Hosting other apps' widgets (AppWidgetHost) is future work — the Home widget is built-in only. Phone-specific shapes and glass rendering will be refined after on-device feedback; several themes currently share the same card geometry. Global replacement of Android system UI is not promised.

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

## Home-screen widget pack

Ten real Android home-screen widgets (one shared helper, `WidgetKit`, with one provider, layout and appwidget-provider XML per widget), so they can be placed on the phone's **default launcher** — Vivo Home / OriginOS — without setting this app as the default Home:

- **Add them:** on the default Home screen, long-press an empty area → Widgets → find the widget by name → drag it onto the Home screen. Every widget can be resized (minimum 110dp).
- **The 10 widgets:**
  1. **Analog Clock** — small 2x2, light face, glass card.
  2. **Analog Clock Dark** — small 2x2, dark navy face with white hands.
  3. **Analog Clock + Weather** — medium 4x2: analog clock on the left, date and live weather on the right.
  4. **Digital Clock + Weather** — medium 4x2: big digits, date and live weather.
  5. **Digital Clock** — small 2x2: big digits and date, no weather.
  6. **Weather** — medium 4x2: big current temperature, condition and today's high/low, no clock.
  7. **Analog Clock + Date** — small 2x2: minimal dial (hour and minute hands only) with the date below.
  8. **Big Clock + Weather** — large 4x4: big analog clock, digital time, date and live weather.
  9. **Mechanical Clock** — small 2x2: clean analog face drawn in code — white dial straight on the wallpaper (transparent, no tile), black Arabic numerals, black hour/minute hands and a thin orange second hand, exactly like the iPhone clock widget. Redrawn once a second by an AlarmManager tick (no wake-ups, stops when the last instance is removed).
  10. **Clock + Weather** — medium 4x2: the same transparent clock face on the left and a blue iPhone-style weather panel on the right — location, big temperature, condition, high/low ("H:26° L:18°") and the sunset/sunrise line, in one widget. The clock redraws once a second; the weather refreshes every 30 minutes from the cached reading.
- **Clocks:** analog hands are drawn by Android from the system time (white dial with tick marks, dark hour/minute hands, orange second hand — all drawn from scratch in this repo); digital digits use a self-updating TextClock. The hands and digits always show the real time.
- **Weather (detailed, not just a temperature line):** the same Open-Meteo source as the built-in widget (free, no key), current coarse location once per refresh, Okara (30.81, 73.45) as the labelled fallback ("Okara · fallback"). Offline it shows the last cached reading; with no reading at all it says "Weather unavailable" — the line is never blank or hidden. What each weather widget shows depends on its size: the Weather widget and the Big Clock + Weather widget show the current temperature, condition, high/low, feels-like, humidity, wind speed + compass direction, rain chance and the sun line; the Big widget also shows pressure and cloud cover. The Analog Clock + Weather and Digital Clock + Weather widgets show the temperature, condition, high/low, feels-like, wind and the sun line in compact 2-column rows. The Clock + Weather widget shows the big temperature, condition, high/low and the sun line on its blue panel. A missing value shows "—", never a blank.
- **Sunrise/sunset line:** every weather widget has a sun line with its own sunrise/sunset icon. Before sunrise it shows today's sunrise ("Sunrise 6:23 AM"); during the day it shows today's sunset ("Sunset 5:20 PM"); after sunset it shows tomorrow's sunrise ("Sunrise tomorrow · 6:23 AM"). Times are 12-hour AM/PM. With no sun data the line says "Sunrise/Sunset —".
- **Location permission:** a widget cannot ask for a runtime permission itself. Tap a weather widget once to open the Weather screen (or a clock widget to open the app), allow location when Android asks (coarse only, asked once), and the widgets show the current location's weather. Deny it and they use the Okara fallback. No fine location, no continuous or background tracking, no stored coordinates. The app refreshes the widgets when it resumes, so granting permission updates them right away.
- **Updates:** every 30 minutes (Android's minimum widget update period) for the date and weather; the clock hands and digits move on their own between updates.

What they are **not**: these are our own widgets, built from scratch in this repo — they are **not Apple’s official widgets** and they do not copy Apple’s code or assets. They are not a lock screen, they do not replace the system shade, and they do not host other apps’ widgets.

## Weather screen

A full Weather screen (`WeatherActivity`, glass/dark design) opens when you tap a weather widget, or tap the weather line on Home:

- **Now:** big current temperature, condition and its icon (our own drawables), location label ("Current location" or "Okara · fallback") and the date.
- **Details grid:** feels-like, high/low, humidity, wind speed + compass direction, pressure, cloud cover, precipitation, rain chance and UV index. A missing value shows "—".
- **Sunrise & Sunset:** both times, a sun arc showing where the sun is in the day, and the next-event line (sunrise before sunrise, sunset during the day, tomorrow's sunrise after sunset).
- **Hourly:** the next 12 hours — time, condition icon, temperature and rain chance.
- **7-day forecast:** day name, condition icon, rain chance, high/low.
- **Offline:** the last cached data is shown. **Refresh weather** reloads the feed; with no data at all the screen says "Weather unavailable" instead of staying blank.

The Weather screen is our own design, built from scratch in this repo — it is not Apple's official weather app and it copies no Apple code or assets.


## Mobile control-center styling

Icon-only Home toolbar, icon dock, theme-owned glass bevels, compact notification action rows and full-height level capsules replace the initial text-button prototype. Where Android reports cross-window blur support, the panel blurs the app behind it; otherwise it uses a readable solid palette. Android remains responsible for the system shade and protected dialogs. The accessibility handle is an optional alternate entry gesture, not a replacement SystemUI implementation.

## Restoring the initial test preferences

After retaining an explicit XML backup from the debug test installation, use Launcher settings → Import saved settings (.xml). Only whitelisted Home/dock, wallpaper URI, selected style and per-theme appearance values are imported; permissions, notification data and signing material are excluded. The file is size-bounded and parsed before preferences are changed. Android-controlled permissions and persisted photo access must be granted separately after a signature-change reinstall.
