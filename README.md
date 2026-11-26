# PH Sat Finder — Native Android

Native Android rebuild of PH Sat Finder. The previous WebView/HTML implementation is replaced by a fully native Java UI and Android sensors.

## Native features

- No HTML / WebView / JavaScript runtime.
- Custom Canvas UI for a stable native viewport.
- Android `SensorManager` compass using the rotation-vector sensor with accelerometer/magnetometer fallback.
- Native GPS using `LocationManager`.
- Native satellite selector popup.
- Native compass dial, target bearing, turn guidance, elevation, LNB skew and LNB clock.
- Same dark/cyan PH Sat Finder visual language and v7 alignment logic.
- App icon retained.

## Build configuration

- Minimum SDK: Android 10 (API 29)
- Compile SDK: Android 16 (API 36)
- Target SDK: API 36
- AGP: 9.4.0
- Gradle: 9.7.1
- JDK: 17

## GitHub Actions

The workflow builds a debug APK on every push to `main` and uploads:

`PH-Sat-Finder-native-debug-APK`

## Permissions

- Fine/coarse location for GPS.
- No motion permission is required for Android SensorManager sensors.

## Important

This project is intended to replace the existing WebView version after testing. Keep the existing `v1.0.0` release untouched until the native build has been installed and verified on the phone.
