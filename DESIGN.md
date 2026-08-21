# MyDictionary Design System

## 0. Research Log

- Embedded references: shortlisted Notion (warm, paper-like reading), Claude (literary editorial tone), and Mintlify (long-form documentation clarity); selected `minimalist-skill.md` + `notion.md` because a learner dictionary needs quiet hierarchy, warm surfaces, and high reading contrast more than expressive decoration.
- Lazyweb: ran 3 mobile queries (`dictionary mobile word definition`, `vocabulary learning mobile detail`, `translation quick lookup mobile dialog`) and viewed 6 shipped screens from Grammarly, Google, Google Translate, Duolingo, Memrise, and GRE Vocab. Harvested a single dominant search action, large headword typography, immediate pronunciation access, progressive detail, inline recovery, and sheet/dialog containment. No pixels, logos, or copy are reused.
- UI/UX database: the learner-dictionary design-system query recommended a minimal single column and complete light/dark support; the accessibility query reinforced announced errors, adjacent recovery, stable type scales, and explicit loading; the Jetpack Compose query reinforced content descriptions, reusable layouts, and lazy lists.
- Imagen drafts: `.omo/evidence/task-2-android-learner-dictionary/research/concept-reading-desk.png` and `.omo/evidence/task-2-android-learner-dictionary/research/concept-night-library.png`; selected `concept-reading-desk.png` as the reference-fidelity contract because its search -> word -> sound -> definition -> example scan path is clearest. The contract covers hierarchy, geometry, palette, and interaction anatomy; the generated device bezel and compressed multi-state montage are presentation chrome, not app UI. The alternate draft contributes only dark-theme contrast and a restrained index-marker idea.
- Merriam-Webster branding: API content must feature the unmodified Merriam-Webster logo at an approved size with the registration symbol visible. Until the official asset is added in the documentation/publication task, the primitive defines a reserved attribution row and exact company naming; it does not draw a substitute logo.
- Skipped lanes: none.

## 1. Atmosphere & Identity

MyDictionary feels like a well-kept reference book on a quiet reading desk: immediate, calm, and precise. The signature is the **learner margin**: a narrow semantic marker beside definitions, examples, and notices that gives each kind of information a stable place without turning the page into nested cards. Warm paper surfaces reduce glare; one deep ink-blue accent identifies actions.

The primary journey is: find a word, hear it, understand one plain definition, see it used, then choose whether to explore more. The interface never asks a learner to parse the provider's raw structure.

Design principles:

1. Meaning before metadata: the first definition precedes secondary senses and related vocabulary.
2. One obvious action per region: search, listen, retry, or expand; never several equal CTAs.
3. Paper, not glass: tonal surfaces and whisper dividers; no gradients, blur, or decorative depth.
4. Recovery is part of the lesson: every error states what happened and what the learner can do.
5. Content determines height: definitions wrap and scroll; they are never truncated to preserve a card shape.

Inclusive personas:

- **New learner**: unfamiliar with IPA and dictionary abbreviations; passes when the plain definition and example are understandable without opening extra sections.
- **Low-vision learner**: uses dark theme and 200% font size; passes when all content reflows into one column with no clipped primary action.
- **Distracted reader**: opens Quick Define while reading another app; passes when the selected word and first meaning are obvious and dismissal is immediate.
- **Screen-reader learner**: navigates by spoken semantics; passes when word, part of speech, pronunciation availability, state, and controls are announced in reading order.

## 2. Color

All implementation colors are semantic Kotlin tokens. Raw color values belong only in the theme mapping, never in components.

| Role / Kotlin token | Light | Dark | Usage |
| --- | --- | --- | --- |
| `Canvas` | `#F7F5EF` | `#191816` | App and dialog background |
| `Surface` | `#FFFFFB` | `#24231F` | Search and definition surfaces |
| `SurfaceMuted` | `#EFECE4` | `#302E29` | Examples, disabled areas, skeletons |
| `Ink` | `#24221F` | `#F2EEE5` | Headwords and body text |
| `InkSecondary` | `#625E57` | `#C9C3B8` | Labels, IPA, metadata |
| `InkDisabled` | `#8C877E` | `#8E887E` | Disabled content with explicit semantics |
| `Outline` | `#C9C3B8` | `#514D45` | Whisper borders and dividers |
| `OutlineStrong` | `#817B72` | `#756F65` | Focus-independent structural edge |
| `Action` | `#3159A7` | `#AEC6FF` | Search focus, links, primary controls |
| `OnAction` | `#FFFFFF` | `#123368` | Content on action surfaces |
| `ActionContainer` | `#DFE7FA` | `#263E6B` | Selected chip and subtle focus state |
| `Example` | `#3E533E` | `#B8D0B5` | Example marker/text accent |
| `ExampleContainer` | `#E8EBDD` | `#283329` | Example surface |
| `Error` | `#B3261E` | `#FFB4AB` | Error icon/text |
| `ErrorContainer` | `#F9DEDC` | `#5F1512` | Recoverable error surface |
| `Warning` | `#7A4F00` | `#F3C56B` | Missing optional content |
| `WarningContainer` | `#F5E5BE` | `#493100` | Missing audio/example notice |
| `Scrim` | `#000000` at 52% | `#000000` at 68% | Dialog isolation |

Rules:

- `Action` is reserved for interaction and focus, never decoration.
- Error and warning always include words or a vector symbol; color is never the only cue.
- Body text targets WCAG 2.2 AA (4.5:1), large text and UI glyphs 3:1. The selected light and dark text pairs exceed those floors.
- Dark theme is independently mapped, not mechanically inverted.

## 3. Typography

Typography uses Android platform families so it works offline and respects system font scaling. A serif is limited to the headword and compact dialog title; all explanatory and interactive copy uses the platform sans family.

| Kotlin style | Family | Size | Weight | Line height | Usage |
| --- | --- | --- | --- | --- | --- |
| `displayLarge` | Serif | 40sp | 700 | 48sp | Headword on wide surfaces |
| `displayMedium` | Serif | 32sp | 700 | Headword on compact surfaces |
| `headlineSmall` | Sans | 24sp | 600 | Showcase or section title |
| `titleLarge` | Serif | 22sp | 700 | Quick Define word/title |
| `titleMedium` | Sans | 18sp | 600 | State and expansion labels |
| `bodyLarge` | Sans | 18sp | 400 | Primary learner definition |
| `bodyMedium` | Sans | 16sp | 400 | Examples and normal copy |
| `labelLarge` | Sans | 14sp | 600 | Buttons and chips |
| `labelMedium` | Sans | 12sp | 600 | Part of speech and metadata |

Rules:

- All dimensions are `sp`; system font scale is never overridden.
- Body copy is at least 16sp. Optional metadata may use 12sp only with comfortable contrast.
- Headwords wrap instead of ellipsizing. At 200% font size, the layout becomes a single column.
- IPA is announced with a descriptive prefix in semantics; visual slashes remain in the text.
- The type system uses two families and four weights at most.

## 4. Spacing & Layout

All spacing derives from a 4dp base.

| Kotlin token | Value | Usage |
| --- | --- | --- |
| `Space1` | 4dp | Tight inline relation |
| `Space2` | 8dp | Icon/label and chip gaps |
| `Space3` | 12dp | Compact control padding |
| `Space4` | 16dp | Phone gutter and normal card inset |
| `Space5` | 20dp | Comfortable content group |
| `Space6` | 24dp | Major card inset or section gap |
| `Space8` | 32dp | Large section break |
| `Space10` | 40dp | Showcase group separation |
| `Space12` | 48dp | Minimum touch target and major rhythm |

Shape tokens: `RadiusSmall` 4dp for functional controls, `RadiusMedium` 8dp for rows, `RadiusLarge` 12dp for cards/dialogs, and `RadiusChip` 50% only for chips.

Adaptive rules:

- Compact phone (<600dp): 16dp side gutters, one column, pronunciation control below/wrapped beside metadata as space allows.
- Large phone (600dp is not assumed): keep the same reading column and allow extra breathing room; do not stretch paragraph measure beyond 42em-equivalent.
- Tablet/window (>=600dp): center content at a maximum 720dp; use 24-32dp gutters. Primitive showcase state groups may form two columns only when each retains at least 320dp.
- Quick Define dialog: 280dp minimum and `min(window width - 32dp, 560dp)` maximum, content-driven height capped by the window with a single vertical scroll owner.
- Insets: screen content respects status/navigation bars; dialog content remains clear of system gestures.
- Stress contract: longest headword/definition, missing example/audio, blank state, and 200% font scale must not create horizontal scrolling or hidden controls.

## 5. Components

### Search Input

- **Structure**: visible label, text field, clear action when non-empty, submit through keyboard/search action.
- **Variants**: empty, populated, focused, disabled, error.
- **Spacing**: `Space3` internal, `Space4` outer; minimum height `Space12`.
- **States**: focus uses `Action`; error uses `Error` plus a plain-language message; disabled uses native disabled semantics.
- **Accessibility**: persistent label, state/error announced, clear button labelled with the current query.
- **Motion**: native state-layer color transition only; no layout movement.
- **Layout**: full-width cluster that wraps supporting text.

### Word Header

- **Structure**: headword, part of speech, IPA, optional pronunciation control.
- **Variants**: full audio, playing/loading, audio unavailable, no IPA.
- **Spacing**: `Space2` metadata cluster, `Space4` to definition.
- **States**: pronunciation control has enabled/loading/playing/error/disabled semantics; unavailable uses a textual notice instead of a dead button.
- **Accessibility**: merges the word and grammar metadata only where it improves reading; pronunciation remains a separately focusable action.
- **Motion**: 120ms state-color response; reduced motion uses the same immediate semantic state without animated progress.
- **Layout**: wrapping cluster on compact widths; no fixed one-line assumption.

### Definition Block

- **Structure**: learner margin marker, optional sense number, plain definition, zero or more examples.
- **Variants**: first sense, additional sense, missing example, long content.
- **Spacing**: `Space3` between marker and copy, `Space2` between definition and examples.
- **States**: normal, expanded/collapsed for additional senses, empty fallback.
- **Accessibility**: sense position is announced; quote decoration is excluded from semantics.
- **Motion**: expansion is a 220ms content fade/size transition; reduced motion switches immediately.
- **Layout**: one readable text column; examples are inset, never nested in another card.

### Pronunciation Control

- **Structure**: consistent vector speaker glyph plus the text `Listen`, `Playing`, or `Try audio again`.
- **Variants**: compact icon+label and dialog icon-only-with-label semantics.
- **Spacing**: `Space2` icon gap; minimum 48dp hit area.
- **States**: default, pressed, focus, loading, playing, error, disabled/unavailable.
- **Accessibility**: explicit word-specific content description and state; never autoplay.
- **Motion**: opacity/state-layer only. Progress animation stops under reduced motion.
- **Layout**: intrinsic-width control that may wrap below metadata.

### Example Block

- **Structure**: narrow `Example` learner-margin marker and italic-free sentence text.
- **Variants**: present, multiple, missing.
- **Spacing**: `Space3` horizontal, `Space2` vertical.
- **States**: present or omitted; missing examples use a concise optional-content notice only in state demonstrations.
- **Accessibility**: announced as `Example: ...`; punctuation is preserved.
- **Motion**: none.
- **Layout**: tonal row with no nested elevation.

### Learning Chip

- **Structure**: short text in a flow row.
- **Variants**: suggestion, synonym, selected, disabled.
- **Spacing**: `Space2` x `Space1`; at least 48dp touch height for actionable suggestions.
- **States**: native default/pressed/focus/disabled; selected uses `ActionContainer` plus a check-free text state where needed.
- **Accessibility**: selectable/action semantics state the result of activation.
- **Motion**: 120ms state color; no scale that shifts neighbors.
- **Layout**: wrapping flow; never horizontal-scroll-only for core suggestions.

### Status Panel

- **Structure**: semantic vector mark, title, concise explanation, optional recovery action.
- **Variants**: loading, empty/no match, suggestion, configuration, offline, quota, parse/server error, partial success.
- **Spacing**: `Space4` inset, `Space3` between explanation and action.
- **States**: status role and action availability are explicit; loading has stable reserved space.
- **Accessibility**: error/status is a live-region announcement; retry is the next focusable item.
- **Motion**: content crossfade up to 200ms; disabled under reduced motion.
- **Layout**: inline with the affected content, not a blocking global overlay.

### Expansion Row

- **Structure**: title, optional summary, consistent disclosure glyph.
- **Variants**: collapsed, expanded, loading, scoped error, disabled.
- **Spacing**: `Space4`; minimum 48dp target.
- **States**: expanded semantic state is announced; loading does not accept duplicate input.
- **Accessibility**: role button, expanded/collapsed state, full-row target.
- **Motion**: 220ms content reveal; immediate under reduced motion.
- **Layout**: full-width row followed by a single content region.

### Quick Define Card

- **Structure**: dismiss control, word header, first definition, optional example, pronunciation, attribution row.
- **Variants**: loading, success, suggestion, bounded error, missing audio/example.
- **Spacing**: `Space5` phone inset, `Space6` tablet inset.
- **States**: dismiss always available; errors keep the selected word visible when possible.
- **Accessibility**: initial focus goes to the word/title, dismiss is labelled, focus remains inside while visible, back/outside dismissal is predictable.
- **Motion**: dialog enters with Material platform motion; reduced motion uses a fade/no transform.
- **Layout**: elevated surface over a scrim with one vertical scroll owner; it never launches a second full-screen flow.

### Merriam-Webster Attribution Row

- **Structure**: approved, unmodified logo asset at an allowed size plus exact provider/product naming.
- **Variants**: normal screen and compact dialog.
- **Spacing**: `Space3` around the logo and label.
- **States**: informational only unless a documented link is provided.
- **Accessibility**: logo description includes `Merriam-Webster`; decorative registration mark is not separately announced.
- **Motion**: none.
- **Layout**: after provider content, never competing with the definition.

## 6. Motion & Interaction

| Token | Duration | Android easing | Usage |
| --- | --- | --- | --- |
| `MotionMicro` | 120ms | standard decelerate | Press/focus/state layer |
| `MotionStandard` | 220ms | standard | Expansion and state replacement |
| `MotionDialog` | 280ms | emphasized decelerate | Quick Define entrance |

- Motion communicates state or spatial relationship only. There is no decorative ambient motion.
- Only alpha, translation, and scale are animated; no repeated layout-thrashing animation.
- Touch feedback starts within 100ms and all controls use native ripple/state layers.
- Reduced motion disables repeated progress animation and transform-based entrances; state labels remain visible so meaning is never animation-dependent.
- Loading longer than 300ms receives visible progress. The learner can still dismiss Quick Define.
- Every control is at least 48x48dp and has at least 8dp separation from another independent target.

## 7. Depth & Surface

Strategy: **tonal shift plus whisper outline**.

| Level | Treatment | Usage |
| --- | --- | --- |
| `Flat` | `Canvas`, no border or shadow | Page background |
| `Grouped` | `Surface`, 1dp `Outline`, no shadow | Search, status, definition grouping |
| `Raised` | `Surface`, 1dp `Outline`, 2dp tonal elevation | Menus and focused showcase samples |
| `Dialog` | `Surface`, 1dp `Outline`, 6dp tonal elevation + `Scrim` | Quick Define |

- Cards never stack inside cards for decoration.
- Light theme may use a very subtle paper-grain concept reference, but production implementation remains code-native and texture-free until a licensed asset is intentionally added.
- Focus visibility does not depend on elevation.

## 8. Accessibility Constraints & Accepted Debt

Constraints:

- WCAG 2.2 AA contrast floors: 4.5:1 normal text, 3:1 large text and meaningful UI glyphs.
- Android TalkBack order matches visual order. Icon-only controls require word-specific labels.
- Error, loading, playing, disabled, selected, expanded, and unavailable states are semantic, not merely visual.
- 200% font scale must preserve the word, primary definition, retry/dismiss actions, and vertical scrolling without horizontal clipping.
- Color is never the sole carrier of state. Examples, errors, and suggestions have text labels or vector marks.
- Touch targets are at least 48x48dp. No primary action sits under system bars or gesture areas.
- Reduced motion preserves every state change and recovery route.
- Plain language avoids unexplained abbreviations. IPA is supplementary, not the sole pronunciation guidance.
- Quick Define can be dismissed at any point, including loading or audio playback.

Accepted debt:

| Item | Location | Why accepted | Owner / Exit |
| --- | --- | --- | --- |
| Official Merriam-Webster logo asset is not yet in the primitive showcase | Attribution row | The publication/documentation task owns downloading and verifying the official unmodified asset and its required registration mark. No substitute logo is drawn. | Todo 10; must be resolved before publication |
| Automated screenshot baselines are device-specific rather than pixel-identical across OEM fonts | Primitive showcase QA | The system font and font rasterizer vary across API/device images; geometry, semantics, clipping, and reference direction are the enforced contract. | Todo 9 visual QA; retain per-device evidence |
