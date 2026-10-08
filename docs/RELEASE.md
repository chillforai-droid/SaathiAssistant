# Signed release builds

Debug APKs (`Saathi-debug-apk`) are fine for testing. For an APK you share or publish, build a **signed release**.
Never commit keystores, passwords or `keystore.properties` — `.gitignore` already blocks `*.jks`, `*.keystore`, `keystore.properties`, `local.properties`.

## 1. Generate a keystore locally (once)
```bash
keytool -genkeypair -v -keystore saathi-release.jks -alias saathi \
  -keyalg RSA -keysize 4096 -validity 10000
```
Remember the keystore password, alias and key password. **Back the file up** — if you lose it you cannot update an installed app.

## 2. Convert it to base64
```bash
base64 -w 0 saathi-release.jks > saathi-release.jks.b64     # Linux
base64 -i saathi-release.jks | tr -d '\n' > saathi-release.jks.b64   # macOS
```

## 3. Add GitHub repository secrets
GitHub repo → Settings → Secrets and variables → Actions → New repository secret:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | contents of `saathi-release.jks.b64` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | `saathi` (your alias) |
| `ANDROID_KEY_PASSWORD` | key password |

## 4. Run the release workflow
- Actions → **Android Release APK (signed)** → *Run workflow*, **or**
- push a tag: `git tag v1.0.0 && git push origin v1.0.0`.

> The release workflow runs on manual dispatch and `v*` tags (not on every push to `main`), so a missing secret never breaks everyday pushes.
> If any secret is missing the job **fails with a clear error** and never produces an unsigned release.

## 5. Download the APK
Open the finished run → **Artifacts** → `Saathi-release-apk` → unzip → `app-release.apk`.

## Local signed build (optional)
```bash
export SAATHI_KEYSTORE_PATH=/path/to/saathi-release.jks
export SAATHI_KEYSTORE_PASSWORD=...
export SAATHI_KEY_ALIAS=saathi
export SAATHI_KEY_PASSWORD=...
./gradlew assembleRelease
```
Release builds use R8 minification (`app/proguard-rules.pro` keeps kotlinx.serialization classes). Test the release APK once before sharing it.
