# Saathi AI — Android automation assistant (साथी)

A **privacy-first, offline-first** native Android app (Kotlin + Jetpack Compose + Material 3). Speak or type a command in **Hindi or English**, launch apps, open settings, call (after confirmation), and build **reusable workflows** that tap/type/scroll through other apps via Android's Accessibility API.

- Works **without any AI/API key**. Commands are understood by local rules.
- No analytics, no ads, no cloud, no `INTERNET` permission.
- Sensitive actions (send / call / share / delete / post) **always ask first**. Passwords, OTPs, PINs and payments are **blocked**.

> Honest scope: Saathi has a generic engine plus app adapters. It does **not** claim to control every third-party app 100%. When a step can't be done reliably it stops and says why.

## Screenshots
| Home | Workflows | Editor | Accessibility |
|---|---|---|---|
| _placeholder_ | _placeholder_ | _placeholder_ | _placeholder_ |

## Features
Voice (SpeechRecognizer, hi-IN/en-IN) · typed commands · quick actions · command preview · workflow create/edit/reorder/duplicate/enable/test · JSON import/export/share · automation history · permissions screen · accessibility setup + on-device inspect demo · app profiles (installed status) · safety & privacy screens · English/Hindi UI.

## Architecture
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). In short: `CommandParser → CommandInterpreter → ActionPlan → ActionEngine → AppAdapter → Intent / Accessibility`.

## Requirements
JDK 17, Android Studio Koala+ (or just the command line), Android SDK 34. Pinned: Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24, Compose compiler 1.5.14, minSdk 26, target/compile 34.

## Open in Android Studio
*File → Open →* this folder → wait for Gradle sync → run the `app` configuration.

## Build locally
```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew test                 # unit tests
./gradlew lint
```
Windows: `gradlew.bat assembleDebug`. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions APK build
1. Push this repo to GitHub.
2. Actions → **Android Debug APK** → *Run workflow* (also runs on every push to `main`).
3. Open the run → **Artifacts → `Saathi-debug-apk`** → unzip → `app-debug.apk`.

`validate.yml` runs `test`, `lint`, `assembleDebug` on every push and pull request.

## Release signing
See [docs/RELEASE.md](docs/RELEASE.md). Uses GitHub Secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Fails clearly when they're missing.

## Enable Accessibility
In the app: *Settings → Accessibility Setup → Open Accessibility settings → Saathi AI → On*. On Android 13+ with a sideloaded APK first allow **restricted settings** (App info → ⋮). Details: [docs/ACCESSIBILITY.md](docs/ACCESSIBILITY.md).

## Supported commands
| Say / type | Result |
|---|---|
| `WhatsApp खोलो`, `व्हाट्सएप खोलो`, `open WhatsApp`, `WhatsApp open करो` | open app |
| `YouTube खोलो`, `यूट्यूब खोलो`, `open YouTube` | open app |
| `सेटिंग खोलो`, `Settings खोलो`, `वाईफाई सेटिंग खोलो` | Android settings (wifi, bluetooth, display, sound, battery, location, accessibility, apps, storage, security) |
| `Rahul को call करो`, `Rahul को फोन करो`, `call Rahul`, `+91… को call करो` | resolve contact → confirm → opens dialer |
| `WhatsApp पर Rahul को Hello भेजो`, `send hi to Rahul on Telegram` | guided send flow, confirm before sending *(needs Accessibility)* |
| `नीचे स्क्रॉल करो`, `scroll up`, `पीछे जाओ`, `home`, `screenshot लो` | scroll / back / home / screenshot *(Accessibility)* |
| `Search पर क्लिक करो`, `click Send`, `hello लिखो` | click visible text / type text *(Accessibility)* |
| `share <text>` | Android share sheet (confirm) |
| `<workflow name or trigger> चलाओ` | run a saved workflow |

Open Camera, Gallery, Files, Phone, Chrome, Facebook, Instagram, Messenger, Telegram the same way.

## Workflows
Create in **Workflows → New**: name, description, trigger phrases, ordered actions (open app/link/settings, dial, back/home/recents, scroll, click text/id, type text, wait, screenshot, share, run another workflow). Per-step *delay before* and *ask confirmation*. Use **Test workflow** before saving. Export shares a `.saathi.json`; import validates everything and rejects blocked steps.

## Safety model
`SAFE` runs directly · `CONFIRM` shows a dialog with the exact action · `BLOCK` never runs (credentials, payments, security changes). It's a conservative keyword/type classifier — a safety net, not a guarantee.

## Troubleshooting
- **"Accessibility service is off"** → enable it (see above); status must read *connected*.
- **Can't enable it on Android 13+** → *Allow restricted settings* in App info.
- **App not found** → package names vary; check *App Profiles*. Install the app or use its visible name.
- **Voice says unavailable/network** → install/enable a speech recognition service and the Hindi language pack in Android settings.
- **Contact not found** → grant Contacts permission, or say the number.
- **Click step fails** → the app's labels differ; edit the workflow's *Click text* (use `A|B` alternatives).
- **Build fails on first CI run** → open the log; the project was written without access to an Android SDK, so a dependency/version tweak may be needed.

## Known limitations
- UI automation of third-party apps depends on their screens; it can break on any app update. Only the parser, validator, safety and JSON logic have JVM unit tests; no automated test drives WhatsApp etc.
- Android may block launching apps from the background (multi-app workflows may stop on some devices).
- Calls open the dialer; the user presses call (no `CALL_PHONE`).
- Hindi parsing is rule-based: unusual phrasings may not parse.
- Voice recognition may use an online service chosen by your device.
- App icon is a placeholder.

## Roadmap
Phase 2: visual recorder, per-app adapters, better Hindi parser, scheduled workflows, notification triggers. Phase 3: optional Gemini/local AI `CommandInterpreter`, contextual multi-step commands. Phase 4: smart routines, device-state triggers, richer editor.
