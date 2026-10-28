# GhostAccess for Android

GhostAccess is an Android app that produces findings from the current device—never seeded or demo data.

## What it checks

- Every launchable installed app Android exposes, including granted sensitive permissions, usage age, installer source, Android target version, debug status, and selected high-impact permission declarations.
- Low-confidence signals are deliberately shown for review; they are not labelled as proof an app is harmful.
- A shared-storage check, after the user explicitly enables Android's **all files** special access. It catalogs reachable shared-storage items and reads compatible text, code, and configuration files for common exposed-secret patterns. Matched values are never shown, saved, or uploaded.
- Optional user-selected documents or one selected folder for a smaller, focused check.

On first launch, Android opens its built-in Usage Access settings. The user must explicitly enable access before an app-permission scan runs. The shared-storage scan separately opens Android's all-files special-access screen and works only if the user enables it. Each app result can open Android's app details page, where the user can review permissions or uninstall the app.

## Important limits

- Android does not allow a regular app to read private files, API keys, passwords, screen content, process memory, or traffic of other apps. All-files access covers shared storage, not other apps' private `Android/data` folders.
- GhostAccess cannot silently revoke another app's permissions. It takes the user to Android Settings for that decision.
- A Google Play release using full app visibility and all-files access must satisfy both the `QUERY_ALL_PACKAGES` and all-files policy declarations. For internal, enterprise, or sideloaded builds, the manifest declarations are already present.

## Build

The native project is in [`android-app`](android-app). Open that folder in Android Studio, allow it to install the Android SDK for API 35 when prompted, then run the `app` configuration on an Android 8.0+ device.

To generate a release APK, create a signing key in Android Studio and use **Build → Generate Signed Bundle / APK**. Use an Android App Bundle (`.aab`) for Google Play.
