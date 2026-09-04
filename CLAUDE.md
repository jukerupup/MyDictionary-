# CLAUDE.md - MyDictionary Project Guide

This file is written for the project owner and for future coding agents. It describes
what the project is, how it is organized, and the rules that keep it reproducible and safe.

## Project purpose and boundaries

MyDictionary is a standalone Android learner's dictionary app. It looks up English words
in Wiktionary (free, no key, no rate limit), shows the definition with an optional
Chinese translation (hidden until tapped), and plays pronunciation audio. It also exposes
a compact `Quick Define` dialog via `ACTION_PROCESS_TEXT` and a floating **bubble** for
on-demand lookup over other apps.

**In scope:** one `:app` module; manual lookup; Quick Define; floating bubble; Wiktionary
definitions/translations/audio; resilient typed errors; tests; Wiktionary (CC BY-SA)
attribution.

**Hard boundaries (do not cross):** no accessibility/clipboard monitoring; no
WorkManager/notification; no Room/history/favorites/accounts/cloud; no Hilt/Koin/
multi-module/navigation framework; no force-push/history rewrite/PR; no API keys or
`.omo`/`.codegraph`/`build` artifacts in Git history.

> Note: the floating bubble uses `SYSTEM_ALERT_WINDOW` + a foreground service — a
> deliberate, user-authorized scope addition (task 11) that supersedes the original
> "no overlay/background service" boundary.

## Architecture and layer responsibilities

```
ui/        Jetpack Compose screens, components, primitives, theme. Never calls APIs directly.
entry/     ProcessTextActivity + ProcessTextViewModel (Quick Define entry point).
bubble/    BubbleService (foreground service) + BubbleOverlay (floating ball + lookup window) + BubbleController.
domain/    Pure Kotlin contracts: repository interfaces, lookup/thesaurus/audio state engines, models.
data/      Wiktionary parsing (en definitions + zh translations), audio URL building, Retrofit/OkHttp repository.
app/       AppContainer: manual dependency wiring + Application class.
```

- `ui/` depends only on `domain/` models and state. Composables receive state + callbacks.
- `data/` converts HTTP/JSON into `domain/model/DictionaryModels.kt`. UI never sees raw DTOs.
- `domain/lookup/LookupStateEngine.kt` implements latest-query-wins and typed failure states.
- `domain/thesaurus/ThesaurusExpansionController.kt` lazily fetches the thesaurus on expand.
- `domain/audio/AudioController.kt` is the interface; `audio/Media3AudioController.kt` is the Media3 impl.
- `bubble/BubbleService.kt` is a LifecycleService + SavedStateRegistryOwner hosting a draggable
  Compose bubble via WindowManager; `bubble/BubbleController.kt` reuses LookupStateEngine.
- `app/AppContainer.kt` wires everything manually (no DI framework).

## Package map

```
io.github.jukerupup.mydictionary
├── MainActivity.kt                     normal lookup entry
├── MyDictionaryApplication.kt          Application + container access
├── app/AppContainer.kt                 manual DI wiring
├── audio/Media3AudioController.kt      Media3 ExoPlayer pronunciation impl
├── data/
│   ├── parser/                         InputNormalizer, WiktionaryParser, ParseContracts
│   ├── remote/WiktionaryApi.kt         en + zh Wiktionary Retrofit endpoints
│   └── repository/WiktionaryDictionaryRepository.kt
├── domain/
│   ├── audio/AudioController.kt        audio contract
│   ├── lookup/LookupStateEngine.kt     latest-query-wins lookup state
│   ├── model/DictionaryModels.kt       DictionaryEntry (with translations), ThesaurusEntry, ...
│   ├── repository/DictionaryRepository.kt  lookupDefinition / lookupThesaurus contract
│   └── thesaurus/ThesaurusExpansionController.kt
├── entry/                              ProcessTextActivity + ProcessTextViewModel (Quick Define)
├── bubble/                             BubbleService, BubbleOverlay, BubbleController
└── ui/
    ├── audio/                          PronunciationControl, PronunciationPlayback
    ├── components/                     QuickDefineCard, SearchInput, StatusPrimitives, WordPrimitives
    ├── lookup/                         LookupScreen, LookupViewModel
    ├── showcase/                       PrimitiveShowcaseScreen (debug design-system showcase)
    ├── thesaurus/ThesaurusSection.kt
    └── theme/                          Color, DesignTokens, Theme, Type
```

## End-to-end data flows

### Normal lookup
1. `MainActivity`/`LookupScreen` collects query → `LookupViewModel.lookup(query)`.
2. `LookupViewModel` → `LookupStateEngine.lookup()` (latest-query-wins, dedupe, cancellation).
3. `LookupStateEngine` → `DictionaryRepository.lookupDefinition(query)`.
4. `WiktionaryDictionaryRepository` → en Wiktionary REST API (definition) + zh Wiktionary
   wikitext (Chinese translation + audio) → `WiktionaryParser` → `List<DictionaryEntry>`
   (headword, functionalLabel, definitions, translations, pronunciation) → `LookupResult`.
5. State flows back through `LookupViewModel` to `LookupScreen` (typed UI states).

### Quick Define
1. Any app fires `ACTION_PROCESS_TEXT` + `text/plain` + `EXTRA_PROCESS_TEXT`.
2. `ProcessTextActivity` reads the extra (read-only), normalizes, and calls its ViewModel.
3. Same `LookupStateEngine`/repository path renders `QuickDefineCard` in a dialog Activity.
4. Close/back/outside-tap finishes with `RESULT_CANCELED`, returning to the source app.

## Domain / API contracts

- `DictionaryRepository.lookupDefinition(query): LookupResult<List<DictionaryEntry>>`
- `DictionaryRepository.lookupThesaurus(query): LookupResult<List<ThesaurusEntry>>`
- `LookupResult<T>` = `Success(value)` | `Suggestions(values)` | `NoMatch` | `Failure(DictionaryError)`.
- `DictionaryError` = `MissingConfiguration(credentials)` | `InvalidCredential | QuotaExceeded | Offline |
  Timeout | NonJsonResponse | Server(statusCode) | MalformedContent(detail)`.
- `DictionaryEntry` carries `translations: List<String>` for the Chinese glosses (hidden by
  default, revealed on tap).
- Thesaurus is fetched ONLY on expansion, cached in memory for the current headword, cancelled
  on headword change, and never blocks or replaces the primary definition. (Wiktionary has no
  separate thesaurus endpoint, so `lookupThesaurus` returns `NoMatch`.)
- Audio: Wiktionary `{{audio|en|FILENAME}}` → `https://en.wiktionary.org/wiki/Special:FilePath/FILENAME`.

## UI state model

`LookupState` = `Idle | Loading(query) | Content(query, entries, thesaurus, audio) | Suggestions(query, list) |
NoMatch(query) | InvalidInput(error) | Failure(query, error)`. UI renders an explicit,
learner-facing recovery message for every non-content state. Configuration-required
(missing keys) is a distinct state that disables search. Thesaurus state rides inside
`Content` as `ThesaurusLoadState` (`NotRequested | Loading | Loaded | Suggestions | NoMatch | Failure`).

## Key and security rules

- API keys come from untracked `local.properties`: `MW_LEARNERS_KEY`, `MW_THESAURUS_KEY`.
- Keys are injected into debug `BuildConfig` only; never logged, never committed,
  never in raw DTOs. Missing/blank key → configuration state, zero outbound requests.
- Release build has empty keys by design.
- `.gitignore` covers `local.properties`, `*.jks`, `*.keystore`, `.gradle/`, `build/`,
  `.idea/`, `.omo/`, `.codegraph/`.

## Testing strategy

- Unit + Robolectric (JVM) tests in `app/src/test/`. No emulator required.
- Parser/repository/state engines: JUnit4 + kotlinx-coroutines-test + MockWebServer (TDD).
- Compose UI/Activity: `createComposeRule`/`ActivityScenario` under Robolectric
  (`RobolectricTestRunner`, `@Config(sdk=[35], qualifiers="w411dp-h891dp")`, `@GraphicsMode(NATIVE)`).
- Real Media3 network playback cannot run on the JVM; the Media3 error path is `@Ignore`d at
  UI level and covered by `Media3AudioControllerTest` with a fake engine.

## Common commands

```powershell
# Full automated gate (JVM, no emulator)
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug

# Just unit tests
.\gradlew.bat testDebugUnitTest

# Targeted tests
.\gradlew.bat testDebugUnitTest --tests "*LookupState*" --tests "*Parser*"
```

Requires JDK 17+ (21 works), SDK platform 37.0 + build-tools 36.0.0, and a `local.properties`
with `sdk.dir` pointing at the SDK. On this machine the SDK lives at `E:\Android\Sdk`
(see the `e-drive-storage` skill).

## Extension recipes

- **Add a UI state**: extend `LookupState` + the recovery copy mapping in `LookupScreen`,
  add a Compose test case, run the targeted test.
- **Add a repository method**: extend `DictionaryRepository`, implement in
  `MerriamWebsterDictionaryRepository` + `MerriamWebsterApi`, test with MockWebServer.
- **Change parsing**: edit `data/parser/`, keep fixtures in the parser test, verify typed
  failure on malformed input.
- **Re-enable emulator QA**: the AVD `mydictionary_api37` is provisioned under
  `E:\Android\.android\avd`; run `emulator -avd mydictionary_api37` on a capable machine,
  then `.\gradlew.bat connectedDebugAndroidTest`.

## Change checklist

1. Keep implementation with its direct tests (one commit per logical change).
2. Never suppress type errors (`as any`, `@ts-ignore`, unchecked casts in prod code).
3. Never commit keys/keystores/build outputs/`.omo`/`.codegraph`.
4. Run `testDebugUnitTest lintDebug assembleDebug` before finishing.
5. No new dependencies without a reason; no background components; keep the must-not list.
