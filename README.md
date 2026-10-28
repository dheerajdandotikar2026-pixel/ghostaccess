# GhostAccess for Android

GhostAccess is an Android app that produces findings from the current device—never seeded or demo data.

## What it checks

- Launchable installed apps with granted sensitive permissions.
- Apps with those permissions that Android reports as unused for 90 days or longer.
- Optional user-selected documents for a small set of common exposed-secret patterns. File contents and matched values are never saved or uploaded.

On first launch, Android opens its built-in Usage Access settings. The user must explicitly enable access before an app-permission scan runs. Each result can open Android's app details page, where the user can review permissions or uninstall the app.

## Important limits

- Android does not allow a regular app to read API keys, private files, screen content, or process memory of other apps.
- GhostAccess cannot silently revoke another app's permissions. It takes the user to Android Settings for that decision.
- A Google Play release using full app visibility must satisfy the `QUERY_ALL_PACKAGES` policy declaration. For internal, enterprise, or sideloaded builds, the manifest declaration is already present.

## Build

The native project is in [`android-app`](android-app). Open that folder in Android Studio, allow it to install the Android SDK for API 35 when prompted, then run the `app` configuration on an Android 8.0+ device.

To generate a release APK, create a signing key in Android Studio and use **Build → Generate Signed Bundle / APK**. Use an Android App Bundle (`.aab`) for Google Play.
