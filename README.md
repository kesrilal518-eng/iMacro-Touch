# iMacro Touch

Android gesture macro app inspired by the *usability* of iPhone AssistiveTouch custom gestures.

**This is not an Apple product and does not use Apple branding or assets.**

## What it does

- Draw continuous gestures on an in-app pad (single stroke)
- Save multiple named macros locally (JSON in SharedPreferences)
- Rename, duplicate, delete macros
- Play back gestures anywhere on screen via **AccessibilityService** + `GestureDescription`
- Speed multiplier (0.25× – 3×) and loop count (1–10)
- Stop control
- Optional floating overlay bubble (requires “Display over other apps”)
- Light / dark Material 3 themes
- Clear permission explanations before opening system Settings

## Honest Android limitations

| Feature | Status |
|---------|--------|
| Record touches that happen *inside other apps* | **Not possible** for normal third-party apps |
| Global multi-touch recording | Not supported |
| Pause mid-gesture | Platform API does not support true pause; Stop cancels |
| Unrestricted multi-stroke simultaneous touch | Limited; this app uses one continuous stroke per macro for reliability |
| Auto-enable Accessibility / Overlay | **Forbidden** – user must grant in system Settings |

Gestures are authored only on the in-app drawing pad, then replayed through the Accessibility service after the user explicitly enables it.

## Requirements

- Android 8.0+ (API 26)
- User must enable the “iMacro Touch” Accessibility service
- Optional: “Display over other apps” for the floating control

## Build (Android Studio)

1. Open the project folder in Android Studio (Ladybug / 2024.2+ recommended).
2. Use JDK 17.
3. Let Gradle sync (wrapper uses Gradle 8.11.1).
4. Run on a device or emulator, or **Build > Build APK(s)**.

Debug APK path after local build:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Build with GitHub Actions

A workflow is included at `.github/workflows/build-apk.yml`.

On every push to `main` (and via workflow_dispatch) it:

1. Sets up JDK 17
2. Builds a debug APK
3. Uploads the APK as a workflow artifact named `app-debug`

Download the artifact from the Actions run page.

## Permissions used

- `SYSTEM_ALERT_WINDOW` – floating bubble only
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` – keep floating service alive with a visible notification
- `POST_NOTIFICATIONS` – Android 13+ notification for the floating service
- Accessibility service (user-enabled) – gesture playback only

No internet permission, no analytics, no ad SDKs, no personal-data collection.

## Project structure (key files)

```
app/src/main/java/com/example/imacrotouch/
  MainActivity.kt          – macro list, playback controls, permissions
  EditorActivity.kt        – draw / save gestures
  GesturePad.kt            – drawing surface (normalized coordinates)
  Macro.kt / MacroStore.kt – model + local persistence
  MacroAccessibilityService.kt – GestureDescription playback
  FloatingMacroService.kt  – overlay bubble + notification
```

## License

MIT – free to use and modify for personal or educational purposes.
