PH SAT FINDER - Android WebView APK project

Target: Android 10 (API 29) and newer.

The v7 HTML interface is bundled locally in the APK, so the app does not need the Python server.
Internet permission is retained because the page may use online resources; GPS and motion sensors are local device features.

Android build configuration:
- Minimum SDK: Android 10 (API 29)
- Target SDK: Android 16 (API 36)
- Compile SDK: Android 16 (API 36)
- Android Gradle Plugin (AGP): 9.4.0
- Gradle: 9.7.1
- JDK: 17
- Android Build Tools: 36.0.0

The project has been successfully built using GitHub Actions with the configuration above.

Build with Android Studio:
1. Open this folder as a project.
2. Make sure JDK 17 is selected.
3. Make sure Android SDK Platform 36 and Build Tools 36.0.0 are installed.
4. Let Gradle sync.
5. Build > Build APK(s).
6. The debug APK will be under:
   app/build/outputs/apk/debug/app-debug.apk

Build with GitHub Actions:
The repository includes a GitHub Actions workflow at:
.github/workflows/build-apk.yml

The workflow automatically builds the debug APK when changes are pushed to the main branch.

Permissions requested:
- Location (fine/coarse) for GPS.
- Internet for web resources.

No separate permission is required for the motion sensors used by the HTML app.

APK:
The successful debug build produces:
app/build/outputs/apk/debug/app-debug.apk
