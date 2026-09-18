PH SAT FINDER - Android WebView APK project

Target: Android 10 (API 29) and newer.
The v7 HTML interface is bundled locally in the APK, so the app does not need the Python server.
Internet permission is retained because the page may use online resources; GPS and motion sensors are local device features.

Build with Android Studio:
1. Open this folder as a project.
2. Let Gradle sync and install the required Android SDK/JDK if prompted.
3. Build > Build APK(s).
4. The debug APK will be under app/build/outputs/apk/debug/.

The project uses AGP 9.4.0 and compileSdk 37. Android Gradle Plugin 9.4 supports API 37 and requires Gradle 9.6/JDK 17.

Permissions requested:
- Location (fine/coarse) for GPS.
- Internet for web resources.
No separate permission is required for the motion sensors used by the HTML app.
