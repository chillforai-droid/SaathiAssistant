# Saathi final verification checklist

## GitHub
1. Push the project to GitHub.
2. Run **Validate** first.
3. Run **Android Debug APK**.
4. Download `Saathi-debug-apk`.
5. For a signed release, configure the four Android signing secrets and run **Android Release APK (signed)**.

## Device
1. Install the APK.
2. Open Saathi → Accessibility Setup → enable Saathi.
3. Test `WhatsApp खोलो`, `YouTube खोलो`, `Settings खोलो`.
4. Test `नीचे स्क्रॉल करो`, `Search पर क्लिक करो`, and `hello लिखो`.
5. Test a message command only after confirming the exact recipient and message in the confirmation dialog.

## Important
- Accessibility automation depends on the target app's current UI.
- The app never handles OTPs, passwords, PINs, CVV or payment actions automatically.
- Calling opens the dialer for final user control.
- A successful build is not the same as guaranteed compatibility with every third-party app version.
