# MyDictionary

MyDictionary is a standalone Android learner's dictionary app. It looks up words in
Merriam-Webster's Learner's Dictionary, plays pronunciation audio, and expands related
vocabulary from the Intermediate Thesaurus. It also exposes a compact `Quick Define`
dialog that any Android app can open on selected text.

- Kotlin + Jetpack Compose (Material 3), single `:app` module
- `minSdk 23`, `compileSdk/targetSdk 37` (Android 17)
- Two entry points: the normal lookup screen and cross-app `ACTION_PROCESS_TEXT` Quick Define
- No accounts, history, offline corpus, background services, or overlays

## Local prerequisites

- JDK 17 or newer (21 is fine)
- Android SDK platform 37.0
- Android SDK Build Tools 36.0.0

Set `JAVA_HOME` and `ANDROID_SDK_ROOT`, then create an untracked `local.properties` file:

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
MW_LEARNERS_KEY=
MW_THESAURUS_KEY=
```

Get your own free Merriam-Webster API keys at <https://dictionaryapi.com/>. A keyless
debug build opens in a configuration-required state and performs **zero** network requests.

## Build and test

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

All tests run on the JVM (unit + Robolectric) - no emulator required. The debug APK is
written to `app\build\outputs\apk\debug\app-debug.apk`.

## Usage

### Normal lookup

1. Open MyDictionary, type a word, press the keyboard search action.
2. The learner definition, part of speech, IPA, and examples appear first.
3. **More meanings** reveals additional senses progressively.
4. Tap the speaker button to hear Merriam-Webster pronunciation audio.
5. **Related words** expands the Intermediate Thesaurus on demand: shared meaning,
   synonyms, related words, near-antonyms, antonyms, phrases, and examples.

### Quick Define (selected text)

1. Select a word or phrase in any Android app that supports text selection.
2. Choose **Quick Define** from the selection toolbar.
3. A compact dialog card shows the learner definition, an example, and audio.
4. Tap **Close**, press back, or tap outside to return to the source app.
   The dialog never replaces your selected text and never launches the full app.

## Testing strategy

- Parser / repository / state engines: JUnit4 + kotlinx-coroutines-test + MockWebServer (TDD).
- Compose UI / Activity / manifest: Robolectric on the JVM (no emulator), including
  every lookup state, Quick Define extras, audio states, thesaurus partial failure/retry,
  accessibility semantics, dark theme, and 200% font scale.
- Live API/audio/thesaurus smoke requires real keys in `local.properties`.

## Troubleshooting

- **"Dictionary setup needed"** - `local.properties` keys are blank; add them and rebuild.
- **"Lookup limit reached"** - Merriam-Webster free keys cap at 1,000 queries/day.
- **"You're offline"** - the app needs network access to dictionaryapi.com.
- **"Dictionary key isn't valid"** - the key was rejected by the API (401/403).
- Missing SDK components - install `platforms;android-37` and `build-tools;36.0.0`
  via sdkmanager.

## Limits

- No offline dictionary, history, favorites, accounts, or cloud sync.
- Thesaurus is fetched only when expanded and cached in memory for the current headword.
- Audio playback stops when the lookup surface closes; no background media service.
- API keys are debug-only build configuration; the release build has empty keys by design.

## Attribution

Dictionary content and audio are provided by **Merriam-Webster, Inc.** under its
Learner's Dictionary and Intermediate Thesaurus APIs. See
<https://dictionaryapi.com/info/terms-of-service> and
<https://dictionaryapi.com/info/branding-guidelines>.

## Developer map

See `CLAUDE.md` for the package map, end-to-end data flows, domain contracts, and
extension recipes.
