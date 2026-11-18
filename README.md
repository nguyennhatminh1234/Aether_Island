# Aether Island

An independent Android Dynamic Island-inspired overlay. Current milestone: **v0.1 — Island Core**.

## Requirements
- Android 10 (API 29) or newer
- Permission to display over other apps
- A persistent foreground-service notification while the Island is running

## Build
The GitHub Actions workflow builds a debug APK on every push to `main` and uploads it as an artifact.

For a local Android Gradle environment:

```bash
gradle :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## v0.1 interactions
- The Island enters as a compact pill.
- Tap the pill to expand; tap again to collapse.
- Swipe upward to hide.
- Tap the small top element to restore Compact.

No notification access, Accessibility Service, root, or Shizuku is used.

## Current limitations
- The activity label is a core placeholder; live music, calls, timers, charging, and notifications are not implemented yet.
- Test on the physical Galaxy Note9 before treating the build as device-verified.
