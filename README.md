# MyDictionary

MyDictionary is a standalone Android learner's dictionary app. It looks up English words,
shows the definition with an optional **Chinese translation** (hidden until you tap it),
and plays pronunciation audio. It also exposes a compact `Quick Define` dialog that any
Android app can open on selected text, plus a floating **bubble** for on-demand lookup
over other apps.

- Kotlin + Jetpack Compose (Material 3), single `:app` module
- `minSdk 23`, `compileSdk/targetSdk 37` (Android 17)
- Data source: **Wiktionary** (free, no API key, no rate limit)
- No accounts, history, or cloud sync

## Data source

Definitions come from the English Wiktionary REST API and Chinese translations from the
Chinese Wiktionary, both part of the Wikimedia Foundation — **free, no API key, and no
query limit** (unlike Merriam-Webster's 1,000/day cap).

| Content | Source |
|---|---|
| English definition + part of speech | `en.wiktionary.org` REST API |
| Chinese translation (hidden by default) | `zh.wiktionary.org` wikitext |
| Pronunciation audio | Wiktionary audio file (`{{audio|en|...}}`) |

## Local prerequisites

- JDK 17 or newer (21 is fine)
- Android SDK platform 37.0
- Android SDK Build Tools 36.0.0

Set `JAVA_HOME` and `ANDROID_SDK_ROOT`, then create an untracked `local.properties` file:

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
```

No API keys are required — the app fetches from Wiktionary directly.

## Build and test

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

All tests run on the JVM (unit + Robolectric) - no emulator required. The debug APK is
written to `app\build\outputs\apk\debug\app-debug.apk`.

## Usage

### Normal lookup

1. Open MyDictionary, type a word, press the keyboard search action.
2. The English definition, part of speech, and examples appear first.
3. Tap **中文解釋** to reveal the Chinese translation (hidden by default).
4. Tap **More meanings** to reveal additional senses.
5. Tap the speaker button to hear pronunciation audio.

### Quick Define (selected text)

1. Select a word or phrase in any Android app that supports text selection.
2. Choose **Quick Define** from the selection toolbar.
3. A compact dialog card shows the definition, an example, and audio.
4. Tap **Close**, press back, or tap outside to return to the source app.
   The dialog never replaces your selected text and never launches the full app.

### Floating bubble

Tap **Bubble** in the app header to show a draggable floating ball over other apps.
Tap the ball to expand a compact lookup window with a text input, definition,
pronunciation, and an "Open full page" link. Grant "display over other apps" when
prompted.

## Testing strategy

- Parser / repository / state engines: JUnit4 + kotlinx-coroutines-test + MockWebServer (TDD).
- Compose UI / Activity / manifest: Robolectric on the JVM (no emulator), including
  every lookup state, Quick Define extras, audio states, accessibility semantics,
  dark theme, and 200% font scale.
- Live smoke uses the real Wiktionary endpoints directly (no key needed).

## Troubleshooting

- **"No match found"** - the word isn't in Wiktionary; check the spelling.
- **"You're offline"** - the app needs network access to wiktionary.org.
- Missing SDK components - install `platforms;android-37` and `build-tools;36.0.0`
  via sdkmanager.

## Limits

- No offline dictionary, history, favorites, accounts, or cloud sync.
- Chinese translations come from Wiktionary's community-maintained pages, so coverage
  is high for common words but may be missing for rare words (the section is simply
  hidden in that case).
- Audio playback stops when the lookup surface closes; no background media service.

## Attribution

Dictionary content, translations, and audio are derived from **Wiktionary**
(<https://www.wiktionary.org/>), licensed under CC BY-SA 4.0. See
<https://creativecommons.org/licenses/by-sa/4.0/>.

## Developer map

See `CLAUDE.md` for the package map, end-to-end data flows, domain contracts, and
extension recipes.
