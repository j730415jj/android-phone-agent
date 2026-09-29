# Android Phone Agent

A privacy-first Android assistant that searches content explicitly selected or shared by the user.

## Current capabilities

- Import UTF-8 text, CSV, HTML, JSON, and XML documents with Android's system file picker.
- Receive text from other Android apps through the Share menu.
- Search, confirm, list, and extractively summarize locally stored content.
- Keep imported content inside the app's private storage.
- Work without an internet connection or broad storage permissions.

## Build

Open the project in Android Studio and run the `app` configuration, or run:

```powershell
gradle assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy boundary

Android does not allow an app to silently search everything on a phone. This app only indexes text that the user selects with the system picker or shares into the app. A future network AI integration should keep provider secrets on a backend, never hard-code them into the APK.
