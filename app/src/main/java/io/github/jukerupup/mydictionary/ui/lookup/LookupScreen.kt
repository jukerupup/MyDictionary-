package io.github.jukerupup.mydictionary.ui.lookup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.jukerupup.mydictionary.app.AppConfiguration
import io.github.jukerupup.mydictionary.data.parser.InputError
import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.model.Definition
import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.thesaurus.ThesaurusExpansionState
import io.github.jukerupup.mydictionary.ui.components.DefinitionBlock
import io.github.jukerupup.mydictionary.ui.components.DictionarySearchInput
import io.github.jukerupup.mydictionary.ui.components.ExampleBlock
import io.github.jukerupup.mydictionary.ui.components.ExpansionRow
import io.github.jukerupup.mydictionary.ui.components.LearningChip
import io.github.jukerupup.mydictionary.ui.components.PronunciationState
import io.github.jukerupup.mydictionary.ui.components.StatusKind
import io.github.jukerupup.mydictionary.ui.components.StatusPanel
import io.github.jukerupup.mydictionary.ui.components.WordHeader
import io.github.jukerupup.mydictionary.ui.theme.DictionaryLayout
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing
import io.github.jukerupup.mydictionary.ui.thesaurus.ThesaurusSection

@Composable
fun LookupScreen(
    state: LookupState,
    onLookup: (String) -> Unit,
    modifier: Modifier = Modifier,
    configuration: AppConfiguration = AppConfiguration.Ready,
    onPronounce: (String) -> Unit = {},
    thesaurusState: ThesaurusExpansionState = ThesaurusExpansionState(),
    onToggleThesaurus: () -> Unit = {},
    onRetryThesaurus: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf(state.queryText()) }
    LaunchedEffect(state.queryText()) {
        val stateQuery = state.queryText()
        if (stateQuery.isNotEmpty()) query = stateQuery
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = DictionaryLayout.MaxReadingWidth)
                    .fillMaxWidth()
                    .testTag("lookup_list"),
                contentPadding = PaddingValues(DictionarySpacing.Space4),
                verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space4),
            ) {
                item {
                    Text(
                        text = "MyDictionary",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                item {
                    DictionarySearchInput(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { onLookup(query) },
                        enabled = !configuration.missingLearnersCredential(),
                        errorMessage = (state as? LookupState.InvalidInput)?.error?.guidance(),
                    )
                }
                if (configuration.missingLearnersCredential()) {
                    item { ConfigurationPanel() }
                } else {
                    when (state) {
                        LookupState.Idle -> item {
                            StatusPanel(
                                kind = StatusKind.Empty,
                                title = "Look up a word",
                                message = "Enter an English word to see a clear learner definition and example.",
                            )
                        }
                        is LookupState.Loading -> item {
                            StatusPanel(
                                kind = StatusKind.Loading,
                                title = "Looking up ${state.query}",
                                message = "Finding the clearest learner definition.",
                            )
                        }
                        is LookupState.Content -> item {
                            LookupContent(
                                entries = state.entries,
                                audioState = state.audio,
                                onPronounce = onPronounce,
                                thesaurusState = thesaurusState,
                                onToggleThesaurus = onToggleThesaurus,
                                onRetryThesaurus = onRetryThesaurus,
                            )
                        }
                        is LookupState.Suggestions -> item {
                            SuggestionsPanel(state.values, onLookup)
                        }
                        is LookupState.NoMatch -> item {
                            StatusPanel(
                                kind = StatusKind.Empty,
                                title = "No match found",
                                message = "Check the spelling or try another form of ${state.query}.",
                            )
                        }
                        is LookupState.InvalidInput -> item {
                            StatusPanel(
                                kind = StatusKind.Error,
                                title = state.error.message(),
                                message = state.error.guidance(),
                            )
                        }
                        is LookupState.Failure -> item {
                            FailurePanel(state.error) { onLookup(state.query) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupContent(
    entries: List<DictionaryEntry>,
    audioState: AudioPlaybackState,
    onPronounce: (String) -> Unit,
    thesaurusState: ThesaurusExpansionState,
    onToggleThesaurus: () -> Unit,
    onRetryThesaurus: () -> Unit,
) {
    val entry = entries.firstOrNull()
    val primaryDefinition = entry?.definitions?.firstOrNull { it.text.isNotBlank() }
    val fallback = entry?.shortDefinition?.takeIf { primaryDefinition == null && it.isNotBlank() }
    val additionalDefinitions = entries.flatMapIndexed { entryIndex, value ->
        if (entryIndex == 0 && primaryDefinition != null) {
            value.definitions.dropWhile { it !== primaryDefinition }.drop(1)
        } else {
            value.definitions
        }
    }
    var expanded by rememberSaveable(entry?.headword) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space4)) {
        if (entry == null || (primaryDefinition == null && fallback == null)) {
            StatusPanel(
                kind = StatusKind.Error,
                title = "We couldn't read this entry",
                message = "Try the word again. The dictionary response did not include a readable definition.",
            )
            return@Column
        }

        val pronunciation = entry.pronunciations.firstOrNull()
        WordHeader(
            word = entry.headword,
            partOfSpeech = entry.functionalLabel,
            ipa = pronunciation?.ipa?.asVisualIpa(),
            pronunciationState = if (pronunciation?.audioReference == null) {
                PronunciationState.Unavailable
            } else {
                audioState.toPronunciationState()
            },
            onPronounce = { onPronounce(pronunciation?.audioReference.orEmpty()) },
        )
        if (primaryDefinition != null) {
            DefinitionWithExamples(primaryDefinition, senseNumber = 1)
        } else if (fallback != null) {
            DefinitionBlock(definition = fallback, senseNumber = 1)
        }

        if (additionalDefinitions.isNotEmpty()) {
            ExpansionRow(
                title = "More meanings",
                expanded = expanded,
                onToggle = { expanded = !expanded },
            )
            if (expanded) {
                Column(
                    modifier = Modifier.testTag("additional_senses"),
                    verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space4),
                ) {
                    additionalDefinitions.forEachIndexed { index, definition ->
                        DefinitionWithExamples(definition, senseNumber = index + 2)
                    }
                }
            }
        }
        ThesaurusSection(
            state = thesaurusState,
            onToggle = onToggleThesaurus,
            onRetry = onRetryThesaurus,
        )
        AttributionRow()
    }
}

@Composable
private fun DefinitionWithExamples(definition: Definition, senseNumber: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2)) {
        DefinitionBlock(definition = definition.text, senseNumber = senseNumber)
        definition.examples.forEach { ExampleBlock(example = it) }
    }
}

@Composable
private fun SuggestionsPanel(values: List<String>, onLookup: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3)) {
        StatusPanel(
            kind = StatusKind.Suggestion,
            title = "Choose a spelling",
            message = "The dictionary found these close matches.",
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
            verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
        ) {
            values.forEach { suggestion ->
                LearningChip(
                    label = suggestion,
                    onClick = { onLookup(suggestion) },
                    modifier = Modifier.semantics {
                        contentDescription = "Look up suggested spelling $suggestion"
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfigurationPanel() {
    StatusPanel(
        kind = StatusKind.Configuration,
        title = "Dictionary setup needed",
        message = "Add the Merriam-Webster Learner's Dictionary key in local.properties, then rebuild the app.",
    )
}

@Composable
private fun FailurePanel(error: DictionaryError, onRetry: () -> Unit) {
    val copy = when (error) {
        is DictionaryError.MissingConfiguration -> Triple(
            "Dictionary setup needed",
            "Add the Learner's Dictionary key in local.properties, then rebuild the app.",
            null,
        )
        DictionaryError.InvalidCredential -> Triple(
            "Dictionary key isn't valid",
            "Check the configured Learner's Dictionary key, then rebuild the app.",
            null,
        )
        DictionaryError.QuotaExceeded -> Triple(
            "Lookup limit reached",
            "The dictionary's request limit has been reached. Try again later.",
            null,
        )
        DictionaryError.Offline -> Triple(
            "You're offline",
            "Reconnect, then try this word again.",
            "Try again",
        )
        DictionaryError.Timeout -> Triple(
            "The lookup took too long",
            "Check your connection and try again.",
            "Try again",
        )
        DictionaryError.NonJsonResponse -> Triple(
            "Unexpected service response",
            "The dictionary returned an unreadable response. Try again.",
            "Try again",
        )
        is DictionaryError.Server -> Triple(
            "Dictionary service unavailable",
            "The service could not complete this lookup. Try again shortly.",
            "Try again",
        )
        is DictionaryError.MalformedContent -> Triple(
            "We couldn't read this entry",
            "Try the word again. The dictionary response was incomplete.",
            "Try again",
        )
    }
    StatusPanel(
        kind = if (error == DictionaryError.Offline) StatusKind.Offline else StatusKind.Error,
        title = copy.first,
        message = copy.second,
        actionLabel = copy.third,
        onAction = onRetry,
    )
}

@Composable
private fun AttributionRow() {
    Text(
        text = "Merriam-Webster's Learner's Dictionary",
        modifier = Modifier
            .fillMaxWidth()
            .testTag("merriam_webster_attribution")
            .semantics {
                contentDescription = "Dictionary content from Merriam-Webster's Learner's Dictionary"
            }
            .padding(DictionarySpacing.Space3),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun LookupState.queryText(): String = when (this) {
    LookupState.Idle, is LookupState.InvalidInput -> ""
    is LookupState.Loading -> query
    is LookupState.Content -> query
    is LookupState.Suggestions -> query
    is LookupState.NoMatch -> query
    is LookupState.Failure -> query
}

private fun AppConfiguration.missingLearnersCredential(): Boolean =
    this is AppConfiguration.MissingCredentials && DictionaryCredential.Learners in credentials

private fun InputError.message(): String = when (this) {
    InputError.Blank -> "Enter a word"
    InputError.TooLong -> "Word is too long"
}

private fun InputError.guidance(): String = when (this) {
    InputError.Blank -> "Type one English word, then search again."
    InputError.TooLong -> "Use 80 characters or fewer."
}

private fun String.asVisualIpa(): String = "/${trim().trim('/')}/"

private fun AudioPlaybackState.toPronunciationState(): PronunciationState = when (this) {
    AudioPlaybackState.Idle, AudioPlaybackState.Completed -> PronunciationState.Available
    AudioPlaybackState.Loading -> PronunciationState.Loading
    AudioPlaybackState.Playing -> PronunciationState.Playing
    is AudioPlaybackState.Error -> PronunciationState.Error
}
