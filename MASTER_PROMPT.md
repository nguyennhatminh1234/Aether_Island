# Aether Island — Master Prompt for Codex

You are working on **Aether Island**, an independent Android app that recreates a customizable Dynamic Island-style experience, inspired by the iPhone Dynamic Island and designed with Material 3 Expressive principles.

Repository: `nguyennhatminh1234/Aether_Island`

## 1. Product goal

Build a polished, lightweight Android overlay app for the user's Samsung Galaxy Note9 (Android 10 / One UI 2.5). The app must be independent: do not rely on DynamicSpot or another third-party island app.

The project must prioritize:
- Smooth, coherent animations rather than abrupt appearance/disappearance.
- A clean, premium visual language inspired by iPhone Dynamic Island, combined with Material 3 Expressive.
- Low RAM, CPU, and battery usage, especially on older devices.
- Compatibility with Android 10+ where technically possible.
- Clear architecture that can grow through the roadmap below.

Do not claim that Android exposes system information or controls that third-party apps cannot access. Explain limitations and implement graceful fallbacks.

## 2. User/device constraints

- Primary test device: Samsung Galaxy Note9, Android 10, One UI 2.5.
- No root.
- No Shizuku.
- Do not use AccessibilityService.
- Avoid Android Studio-dependent workflows where possible; the user works with VS Code and Codex.
- Keep dependencies minimal. Do not add a library just for a small helper function.
- Use Kotlin and Jetpack Compose for app UI unless the existing repository structure gives a strong reason not to.
- Suggested application ID: `vn.aether.island`.
- Suggested minimum SDK: 29 (Android 10), target/compile SDK: use a currently supported stable SDK available in the environment. Check the installed toolchain before changing versions.

## 3. Core interaction model

The Island has three primary states:

### Hide
- Default idle state when there is no active activity such as music, timer, or call.
- Only a very small part should peek down from the top edge, visually subtle, smaller than a camera cutout.
- Tapping it returns to Compact.
- It must not permanently obstruct status-bar interactions.

### Compact
- A small centered pill near the top edge.
- Shows the icon and concise status for the current activity.
- Appears when an activity starts or when the user taps Hide.

### Expanded
- Shows the main activity's richer content.
- Tapping Compact expands it using a spring animation with a slight overshoot, then settles smoothly.
- Swiping upward may hide the Island or switch the active activity, depending on context.
- Touch targets must be practical and gestures must not make normal phone use frustrating.

### Signature entrance animation
When a new activity appears:
1. The small Hide element stretches/drops down from the top edge, like a liquid droplet.
2. Its shape rounds and morphs into the Compact pill.
3. The transition should feel continuous and spring-like.
4. Avoid abrupt toggles between separate UI elements.

## 4. Multiple activities / Activity Stack

When multiple activities exist (for example, music plus a new message):
- Do not create separate floating circular icons beside the Island.
- The primary activity occupies the front layer.
- Secondary activity previews sit behind it, partially obscured, muted, and around 30% opacity. A subtle blur may be used if performance allows.
- Swiping upward on the Island flips the activity card (a 3D-style flip) to make the next activity primary.
- Tapping the Island while multiple activities exist opens the Expanded view for the primary activity. A small strip below it indicates other active activities.
- Swiping upward inside Expanded can move the current primary activity into a compact strip at the top and promote the next activity into the main content area.
- With two or more activities, show a small secondary-activity strip containing their icons.
- Tapping that strip opens a larger Activity Center view listing active activities. Selecting an activity promotes it to primary.
- Changing or dismissing UI must never stop the underlying music, timer, call, or other system activity.

## 5. Architecture and Android permissions

Use Android APIs appropriately:
- `WindowManager` overlay for the Island surface, with a clear permission/onboarding flow for `SYSTEM_ALERT_WINDOW`.
- `MediaSession` / media controller APIs for music information and playback actions where available.
- `NotificationListenerService` for notifications, only in the relevant roadmap version, with an explanation and explicit user enablement.
- `DataStore` for persistent preferences if needed.
- Use foreground services only when justified by Android background-execution rules. Do not keep a service alive unnecessarily.
- Handle overlay permission revocation, process recreation, screen rotation, display cutouts, status-bar insets, and device reboot gracefully.
- Never request Accessibility access.
- Do not silently enable permissions or imply that the app can grant them itself.

Before implementation, inspect current Android APIs and the existing project. Prefer the simplest robust implementation.

## 6. Roadmap — implement only the requested version

Do not jump ahead or implement future-version features unless explicitly requested.

### v0.1 — Island Core
- Project foundation and app launch/onboarding.
- Overlay permission flow.
- WindowManager-based Island overlay.
- Hide, Compact, and Expanded states.
- Tap and basic swipe gestures.
- Hide state when idle.
- Droplet-to-pill entrance morph.
- Spring/overshoot expansion animation.
- Basic settings only if required for testing.
- No notifications, widgets, or full activity dashboard yet.

### v0.2 — Live Activities
- Music, charging, battery, timer, stopwatch, incoming call, headphones, and volume activity surfaces.
- Activity Stack and switching behavior.
- Do not stop underlying activities when changing Island UI.

### v0.3 — Notifications
- NotificationListenerService and explicit notification-access onboarding.
- Message/notification preview behind the primary activity.
- Flip-card interaction, grouping, and swipe-to-dismiss.
- Tapping a notification opens its originating app where Android permits.

### v0.4 — Aether Activities dashboard
This is a standalone dashboard, not Android Quick Settings and not a replacement for the system notification shade.

Sections:
1. **Live Activities (LAc):** rounded tiles such as Music, Timer, Call, Charging, Headphones.
2. **System Events:** rounded tiles such as Volume, Battery, Wi-Fi, Bluetooth, Silent Mode, Hotspot. These primarily display state/information; do not assume they can control every system setting.
3. **My Widgets:** user-added widgets such as Clock, Weather, Calendar, Battery Monitor, Music Visualizer, and Notes. Add/remove/move/resize/reorder.
4. **Notifications:** bottom section with clear visual separation, grouping, dismiss gestures, tap-to-open, and Clear All.

When Live Activities or System Events are opened:
- Those sections expand/overlay upward over the Notifications area.
- Notifications shrink into a smaller visible region at the bottom, showing recent items and remaining scrollable.
- Notifications must not be lost or cleared by this layout transition.
- Closing the upper section restores the normal Notifications size.
- Use smooth spring/slide transitions.

### v0.5 — Widget Engine
- Add, remove, resize, reorder, and arrange widgets.
- Initial widgets: Clock, Weather, Calendar, Music Visualizer.
- Make widget failures isolated; one widget must not crash the dashboard.

### v0.6 — Customization
- Island size and position.
- Transparency and colors.
- Animation speed.
- Light/dark appearance and multiple themes.
- Persist settings.

### v1.0 — Stable
- Optimize RAM, CPU, and battery.
- Restore state after reboot where permitted.
- Handle concurrent activities reliably.
- Polish accessibility of touch targets and contrast.
- Test on Android 10+ and document known limitations.

## 7. Mandatory workflow for every task

1. **Inspect first.** Read the repository structure, existing source, Gradle files, README, and current git state before editing.
2. **Report the current state briefly.** Identify what already exists and what is missing for the requested version.
3. **Plan narrowly.** Make a short implementation plan for the requested version only.
4. **Implement incrementally.** Keep changes focused and avoid unnecessary refactors.
5. **Build and test.** Run the available Gradle build/tests. Fix errors caused by your changes. If the environment cannot build, state exactly why and do not claim success.
6. **Review.** Check permissions, lifecycle handling, memory leaks, overlay cleanup, and whether UI actions accidentally affect underlying activities.
7. **Summarize.** List changed files, implemented behavior, build/test results, remaining limitations, and the next logical task.
8. **Commit only when asked.** Do not push or commit unless the user explicitly requests it.

## 8. Strict implementation rules

- Work only on the version explicitly requested by the user.
- Do not mark a feature complete unless it is implemented and tested.
- Do not replace real functionality with static mock UI while claiming it works.
- Do not add ads, analytics, tracking, accounts, or network calls unless explicitly requested.
- Do not add unrelated features.
- Do not introduce heavy dependencies without explaining why they are needed.
- Do not use AccessibilityService, root, or Shizuku.
- Keep secrets and API keys out of source control.
- Preserve existing working code and user changes.
- If requirements are ambiguous, ask one focused question before making an irreversible design decision.
- Explain technical trade-offs in clear Vietnamese suitable for a Grade 7 student who already understands basic programming concepts.

## 9. First task

When the user asks you to begin, start with **v0.1 — Island Core only**.

First inspect the repository. If it is empty, create the minimal Android project foundation using the constraints above. Then implement the v0.1 scope, build it, and report what works on an emulator versus what still needs testing on the physical Note9.

Do not start v0.2 or later until the user explicitly approves moving forward.
