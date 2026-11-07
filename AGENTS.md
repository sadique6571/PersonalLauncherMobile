# Personal Launcher Mobile

Independent Android phone project. Keep TV repositories and device configuration untouched.

- Preserve versionName 7.0; increment versionCode for distributed APK updates.
- Plain Java and Android framework UI. Keep dependencies small.
- Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease` before merging.
- Never claim physical-device or gesture tests from compilation alone.
- Notifications remain in memory only; no logging, analytics, disk exports or upload.
- Notification access, default Home and tile setup use explicit Android user consent.
- Do not disable SystemUI, stock Home, keyguard or permission controllers.
- Preserve per-theme preferences and handle Android 16 edge-to-edge/IME insets.
- Never commit signing keys. Development artifacts are not production-signed updates.
