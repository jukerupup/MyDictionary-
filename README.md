# MyDictionary

MyDictionary is an Android learner's dictionary scaffold. It targets Android 17 (API 37), supports Android 6.0 and newer (minSdk 23), and uses Jetpack Compose.

## Local prerequisites

- JDK 17
- Android SDK platform 37.0
- Android SDK Build Tools 36.0.0

Set `JAVA_HOME` and `ANDROID_SDK_ROOT`, then create an untracked `local.properties` file:

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
MW_LEARNERS_KEY=
MW_THESAURUS_KEY=
```

API keys are intentionally absent by default. A keyless debug build opens in a configuration-required state and performs no network request.

## Build

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

No credentials, keystores, local build outputs, IDE state, or agent evidence belong in source control.
