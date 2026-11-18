# Aether Island

Android Dynamic Island-inspired overlay. Current milestone: **v0.2 — Live Activities foundation**.

## v0.2 included
- Media session metadata and playback-state detection through Android Notification Access.
- Local 1-minute timer and stopwatch test activities.
- Activity stack list in Expanded state, with horizontal swipe to switch the selected activity.
- Existing Hide / Compact / Expanded interaction retained.
- No root, Shizuku or Accessibility Service.

## Setup
1. Grant Display over other apps.
2. Start Island.
3. For music metadata, enable Aether Island under Android Notification Access settings.
4. Start a test timer or stopwatch from the app, then expand Island.

## Not included yet
Incoming-call detection, headphones/audio-device events, live system volume, charging/battery tiles, and full media transport controls are still planned. This build must be tested on a physical Android 10+ device before being considered device-verified.

## Build
GitHub Actions builds a debug APK on every push to main.