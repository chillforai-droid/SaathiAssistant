# Final build status

This package is the repaired source project based on the uploaded Claude project.

## Repaired in this build
- More reliable Accessibility event tracking during multi-step workflows.
- Better text matching: clickable exact match, exact label fallback, single partial match, and ambiguity protection.
- Resource-ID matching fallback for common Android ID formats.
- Scroll fallback using on-device gestures when an app does not expose a scrollable AccessibilityNodeInfo.
- Visible-screen inspection includes hint text while continuing to exclude password fields.
- Hindi/English message command parsing accepts both send wording and message wording.
- Updated documentation and GitHub verification checklist.

## Build verification
The supplied environment could not download Gradle 8.7 from services.gradle.org because outbound network/DNS access was unavailable. Therefore this package has NOT been falsely marked as locally compiled. The repository already contains GitHub Actions workflows that build the debug APK and signed release APK on GitHub runners.

Run `./gradlew test assembleDebug` locally or use GitHub Actions → Validate / Android Debug APK.
