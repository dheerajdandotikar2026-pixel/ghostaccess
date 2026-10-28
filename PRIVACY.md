# GhostAccess privacy notice

GhostAccess is designed to work on the device. It does not include analytics, advertising, an account system, a network client, or a server.

## Information the app reads

With the user’s explicit Android Usage Access setting enabled, GhostAccess reads the package name, app label, granted permission names, Android-reported last-use time, installer source, Android target version, and selected manifest declarations for launchable installed apps. The app turns these into clearly labelled review signals; a signal is not a claim that an app is harmful.

The optional focused file check reads only documents or a folder the user chooses in Android's system picker. A separate shared-storage check requires Android's all-files special access. It catalogs reachable shared-storage items and reads compatible text, code, and configuration files for a limited set of exposed-secret patterns. It reports the file path plus the type of possible secret. It never displays, saves, or transmits the matched value. Android still prevents GhostAccess from reading other apps' private data folders.

## Sharing and retention

GhostAccess does not send scanned information off the phone. Scan results are displayed in memory and disappear when the app process is closed. It does not create a device scan history.

## Controls

The user can decline Usage Access or all-files access, disable either later in Android Settings, choose individual files or one folder, cancel document selection, and review each flagged app in Android's app settings. GhostAccess never changes another app's permissions itself.
