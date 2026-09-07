package io.github.jukerupup.mydictionary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.jukerupup.mydictionary.data.parser.InputError
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.ui.theme.DictionaryShapes
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing
import io.github.jukerupup.mydictionary.ui.theme.DictionaryLayout

@Composable
fun QuickDefineCard(
    word: String,
    definition: String,
    modifier: Modifier = Modifier,
    example: String? = null,
    audioAvailable: Boolean = true,
    scrollable: Boolean = false,
    onDismiss: () -> Unit = {},
    onPronounce: () -> Unit = {},
) {
    QuickDefineCardContainer(
        modifier = modifier,
        scrollable = scrollable,
        onDismiss = onDismiss,
    ) {
        Text(
            text = word,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        DefinitionBlock(definition = definition)
        example?.let { ExampleBlock(example = it) }
        PronunciationControl(
            word = word,
            state = if (audioAvailable) {
                PronunciationState.Available
            } else {
                PronunciationState.Unavailable
            },
            onClick = onPronounce,
        )
        ProviderAttribution()
    }
}

@Composable
fun QuickDefineCard(
    state: LookupState,
    selectedText: String?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onPronounce: (String) -> Unit = {},
) {
    QuickDefineCardContainer(
        modifier = modifier,
        scrollable = true,
        onDismiss = onDismiss,
        title = "Quick Define",
    ) {
        when (state) {
            LookupState.Idle -> QuickDefineStatus(
                StatusKind.Loading,
                "Preparing lookup",
                "Getting the selected text ready.",
            )
            is LookupState.Loading -> QuickDefineStatus(
                StatusKind.Loading,
                "Looking up ${state.query}",
                "Finding the clearest learner definition.",
            )
            is LookupState.Content -> QuickDefineLearnerContent(state, onPronounce)
            is LookupState.Suggestions -> QuickDefineStatus(
                StatusKind.Suggestion,
                "No exact match for ${state.query}",
                "Close Quick Define and try one of these spellings: ${state.values.joinToString()}.",
            )
            is LookupState.NoMatch -> QuickDefineStatus(
                StatusKind.Empty,
                "No match found",
                "Close Quick Define and try another form of ${state.query}.",
            )
            is LookupState.InvalidInput -> QuickDefineStatus(
                StatusKind.Error,
                state.error.quickDefineTitle(),
                state.error.quickDefineGuidance(),
            )
            is LookupState.Failure -> QuickDefineStatus(
                if (state.error == DictionaryError.Offline) StatusKind.Offline else StatusKind.Error,
                state.error.quickDefineTitle(),
                state.error.quickDefineGuidance(),
            )
        }
        if (state !is LookupState.Content && !selectedText.isNullOrBlank()) {
            Text(
                text = selectedText,
                modifier = Modifier.testTag("quick_define_selected_text"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickDefineCardContainer(
    modifier: Modifier,
    scrollable: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_define_card"),
        shape = DictionaryShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .padding(DictionarySpacing.Space5),
            verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
        ) {
            QuickDefineHeader(title = title, onDismiss = onDismiss)
            Column(
                modifier = Modifier.then(
                    if (scrollable) {
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                ),
                verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
            ) { content() }
        }
    }
}

@Composable
private fun QuickDefineHeader(title: String?, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .focusable()
                    .semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
        }
        TextButton(onClick = onDismiss) { Text("Close") }
    }
}

@Composable
private fun QuickDefineLearnerContent(
    state: LookupState.Content,
    onPronounce: (String) -> Unit,
) {
    val entry = state.entries.firstOrNull()
    val definition = entry?.definitions?.firstOrNull { it.text.isNotBlank() }
    val fallback = entry?.shortDefinition?.takeIf { definition == null && it.isNotBlank() }
    if (entry == null || (definition == null && fallback == null)) {
        QuickDefineStatus(
            StatusKind.Error,
            "We couldn't read this entry",
            "Close Quick Define and try the selection again.",
        )
        return
    }
    val pronunciation = entry.pronunciations.firstOrNull()
    WordHeader(
        word = entry.headword,
        partOfSpeech = entry.functionalLabel,
        ipa = pronunciation?.ipa?.let { "/${it.trim().trim('/')}/" },
        pronunciationState = if (pronunciation?.audioReference == null) {
            PronunciationState.Unavailable
        } else {
            state.audio.toQuickDefinePronunciationState()
        },
        onPronounce = { onPronounce(pronunciation?.audioReference.orEmpty()) },
    )
    DefinitionBlock(definition = definition?.text ?: fallback.orEmpty(), senseNumber = 1)
    definition?.examples?.firstOrNull()?.let { ExampleBlock(example = it) }
    ProviderAttribution()
}

@Composable
private fun QuickDefineStatus(kind: StatusKind, title: String, message: String) {
    StatusPanel(kind = kind, title = title, message = message)
}

@Composable
private fun ProviderAttribution() {
    Text(
        text = "Wiktionary (CC BY-SA 4.0)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun AudioPlaybackState.toQuickDefinePronunciationState(): PronunciationState = when (this) {
    AudioPlaybackState.Idle, AudioPlaybackState.Completed -> PronunciationState.Available
    AudioPlaybackState.Loading -> PronunciationState.Loading
    AudioPlaybackState.Playing -> PronunciationState.Playing
    is AudioPlaybackState.Error -> PronunciationState.Error
}

private fun InputError.quickDefineTitle(): String = when (this) {
    InputError.Blank -> "No word selected"
    InputError.TooLong -> "Selection is too long"
}

private fun InputError.quickDefineGuidance(): String = when (this) {
    InputError.Blank -> "Select a word or phrase, then open Quick Define again."
    InputError.TooLong -> "Select 80 characters or fewer."
}

private fun DictionaryError.quickDefineTitle(): String = when (this) {
    is DictionaryError.MissingConfiguration -> "Dictionary setup needed"
    DictionaryError.InvalidCredential -> "Dictionary key isn't valid"
    DictionaryError.QuotaExceeded -> "Lookup limit reached"
    DictionaryError.Offline -> "You're offline"
    DictionaryError.Timeout -> "The lookup took too long"
    DictionaryError.NonJsonResponse -> "Unexpected service response"
    is DictionaryError.Server -> "Dictionary service unavailable"
    is DictionaryError.MalformedContent -> "We couldn't read this entry"
}

private fun DictionaryError.quickDefineGuidance(): String = when (this) {
    is DictionaryError.MissingConfiguration -> "Add the Learner's Dictionary key, rebuild, and try again."
    DictionaryError.InvalidCredential -> "Check the configured Learner's Dictionary key, then rebuild."
    DictionaryError.QuotaExceeded -> "Try again later."
    DictionaryError.Offline -> "Reconnect, then open Quick Define again."
    DictionaryError.Timeout -> "Check your connection and open Quick Define again."
    DictionaryError.NonJsonResponse -> "Close Quick Define and try again."
    is DictionaryError.Server -> "Close Quick Define and try again shortly."
    is DictionaryError.MalformedContent -> "Close Quick Define and try the selection again."
}

@Composable
fun QuickDefineDialog(
    word: String,
    definition: String,
    example: String?,
    audioAvailable: Boolean,
    onDismiss: () -> Unit,
    onPronounce: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(DictionarySpacing.Space4)
                .testTag("quick_define_dialog"),
            contentAlignment = Alignment.Center,
        ) {
            QuickDefineCard(
                word = word,
                definition = definition,
                example = example,
                audioAvailable = audioAvailable,
                onDismiss = onDismiss,
                onPronounce = onPronounce,
                scrollable = true,
                modifier = Modifier
                    .widthIn(min = DictionaryLayout.MinDialogWidth)
                    .widthIn(max = DictionaryLayout.MaxDialogWidth)
                    .heightIn(max = maxHeight - DictionarySpacing.Space8),
            )
        }
    }
}
