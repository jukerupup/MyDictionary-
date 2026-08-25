# CLAUDE.md - MyDictionary Project Guide

This file is written for the project owner and for future coding agents. It describes
what the project is, how it is organized, and the rules that keep it reproducible and safe.

## Project purpose and boundaries

MyDictionary is a standalone Android learner's dictionary app. It looks up words in
Merriam-Webster's Learner's Dictionary, plays pronunciation audio, and expands related
vocabulary from the Intermediate Thesaurus on demand. It also exposes a compact `Quick
Define` dialog that any Android app can open on selected text via `ACTION_PROCESS_TEXT`.

**In scope:** one `:app` module; manual lookup; Quick Define; Learners Dictionary
definitions/IPA/examples/audio/suggestions; Intermediate Thesaurus on expand; resilient
typed errors; local secret-safe API keys; tests; MW attribution.

**Hard boundaries (do not cross):** no overlay/accessibility/clipboard/background
service; no WorkManager/notification; no Room/history/favorites/accounts/cloud; no
Wordnik/other dictionary sources; no Hilt/Koin/multi-module/navigation framework; no
force-push/history rewrite/PR; no API keys or `.omo`/`.codegraph`/`build` artifacts in
Git history.

## Architecture and layer responsibilities

```
ui/        Jetpack Compose screens, components, primitives, theme. Never calls APIs directly.
entry/     ProcessTextActivity + ProcessTextViewModel (Quick Define entry point).
bubble/    BubbleService (foreground service) + BubbleOverlay (floating ball + lookup window) + BubbleController.
domain/    Pure Kotlin contracts: repository interfaces, lookup/thesaurus/audio state engines, models.
data/      Merriam-Webster parsing, normalization, audio URL building, Retrofit/OkHttp repository.
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
│   ├── parser/                         InputNormalizer, MerriamWebsterParser/Markup/Audio, ParseContracts
│   ├── remote/MerriamWebsterApi.kt     Retrofit endpoints (raw bodies)
│   └── repository/MerriamWebsterDictionaryRepository.kt
├── domain/
│   ├── audio/AudioController.kt        audio contract
│   ├── lookup/LookupStateEngine.kt     latest-query-wins lookup state
│   ├── model/DictionaryModels.kt       DictionaryEntry, ThesaurusEntry, Definition, Pronunciation...
│   ├── repository/DictionaryRepository.kt  lookupDefinition / lookupThesaurus contract
│   └── thesaurus/ThesaurusExpansionController.kt
├── entry/                              ProcessTextActivity + ProcessTextViewModel (Quick Define)
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
4. `MerriamWebsterDictionaryRepository` → `MerriamWebsterApi` (raw body) → `MerriamWebsterParser`
   → `List<DictionaryEntry>` → `LookupResult.Success/Failure`.
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
- Thesaurus is fetched ONLY on expansion, cached in memory for the current headword, cancelled
  on headword change, and never blocks or replaces the primary definition.
- Audio: `sound.audio` + documented `bix`/`gg`/number/first-letter subdirectory rule →
  `https://media.merriam-webster.com/audio/prons/...`.

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
