PH Sat Finder

A mobile satellite dish alignment tool for the Philippines.

PH Sat Finder is an Android application designed to help users align satellite dishes using their phone's GPS location, compass heading, and motion sensors.

The app provides a visual satellite aiming guide to help users find the correct direction and elevation for satellite television setup.

Features

- GPS-based location support
- Phone compass and heading display
- Satellite azimuth and elevation guidance
- LNB skew / rotation guide
- Visual dish alignment interface
- Local HTML interface bundled inside the Android APK
- No Python server required

Supported Satellite Services

The app includes satellite references for:

- Cignal — SES-7
- GSAT — SES-9
- Satlite — Koreasat 7

Android Compatibility

- Android 10 and newer
- Minimum SDK: API 29

Technology

PH Sat Finder uses an HTML-based interface packaged inside an Android WebView application.

The interface is built with web technologies and runs locally within the APK. Android provides access to device features such as GPS and motion sensors.

This project is not a fully native Kotlin interface.

Installation

Download the available APK from the project's GitHub Actions artifacts or Releases, if published.

Install the APK on an Android 10 or newer device and grant the required location permission for GPS functionality.

Build

The project includes a GitHub Actions workflow for building the Android APK.

The debug APK is generated at:

app/build/outputs/apk/debug/app-debug.apk

Project Status

Version 2.0.0

The Android 10+ version is retained as the preferred APK release.

Developer

Created by "Jonardca" (https://github.com/Jonardca)

GitHub Repository: "PH-Sat-Finder" (https://github.com/Jonardca/PH-Sat-Finder)
