# Accessibility service

Saathi uses an Android `AccessibilityService` (`SaathiAccessibilityService`) so that automations *you start* can tap, type and scroll in other apps. Android offers no other way.

## What it does
- Exposes the visible window to `AccessibilityActionExecutor` **only while you have enabled the service**.
- Finds nodes by visible text / content description (`AccessibilityNodeFinder`), clicks them, sets text in editable fields, scrolls, and performs Back / Home / Recents / Screenshot global actions.
- Tracks the foreground package name (to pick an app adapter).

## What it never does
- Stores, logs or uploads screen content. (`visibleTexts()` is used only by the on-device *Inspect* demo and shown on screen.)
- Types into password fields (`isPassword` nodes are refused) or enters OTPs / PINs / payment data (blocked by `SafetyClassifier`).
- Bypasses CAPTCHA, DRM, banking or other security screens.
- Matches editable fields as click targets (so a typed search query can't "find itself").
- Guesses when several matches exist — it stops with an "ambiguous" error.

## Enabling it
1. Settings → Accessibility → Installed/Downloaded apps → **Saathi AI** → On (the app's *Accessibility Setup* screen opens this page for you).
2. **Sideloaded APK on Android 13+:** open *Settings → Apps → Saathi AI → ⋮ → Allow restricted settings*, then enable the service.
3. Return to the app: status should read *Enabled and connected*.

## Config
`res/xml/accessibility_service_config.xml`: events limited to `typeWindowStateChanged`, `canRetrieveWindowContent=true`, flags `flagIncludeNotImportantViews|flagReportViewIds|flagRetrieveInteractiveWindows`. No gestures, no key events, no touch exploration.

## Known limits
- Apps can mark screens secure/hide nodes (banking apps, some video players); those are simply not automatable.
- Labels are language/version specific. Messaging flows try English and Hindi labels (`Search|खोजें`, `Send|भेजें`); other languages need custom workflows.
- Android may refuse to start an app from the background; multi-app workflows can fail on some devices.
