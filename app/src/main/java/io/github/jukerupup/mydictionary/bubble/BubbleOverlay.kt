package io.github.jukerupup.mydictionary.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import io.github.jukerupup.mydictionary.data.parser.MerriamWebsterAudio
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.ui.audio.PronunciationPlayback

/**
 * The floating bubble UI. Collapsed = a small draggable circle. Expanded = a
 * compact lookup card with a text input, definition result, and pronunciation
 * button.
 */
@Composable
fun BubbleOverlay(
    controller: BubbleController,
    audioController: AudioController,
    onDismiss: () -> Unit,
    onMoved: (deltaX: Int, deltaY: Int) -> Unit = { _, _ -> },
    onExpandedChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val state by controller.state.collectAsState()

    if (!expanded) {
        BubbleBall(
            onTap = {
                expanded = true
                onExpandedChanged(true)
            },
            onDrag = onMoved,
            modifier = modifier,
        )
    } else {
        BubbleCard(
            state = state,
            onLookup = controller::lookup,
            onCollapse = {
                expanded = false
                onExpandedChanged(false)
            },
            onDismiss = onDismiss,
            audioController = audioController,
            modifier = modifier,
        )
    }
}

@Composable
private fun BubbleBall(
    onTap: () -> Unit,
    onDrag: (deltaX: Int, deltaY: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .testTag("bubble_ball")
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x.toInt()
                    val dy = dragAmount.y.toInt()
                    onDrag(dx, dy)
                }
            }
            .clickable { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "D",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 22.sp,
            )
        }
    }
}

@Composable
private fun BubbleCard(
    state: LookupState,
    onLookup: (String) -> Unit,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    audioController: AudioController,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val playback = remember(audioController) {
        PronunciationPlayback(audioController = audioController, scope = scope)
    }

    Surface(
        modifier = modifier
            .size(width = 300.dp, height = 360.dp)
            .testTag("bubble_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Quick Define",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BubbleTextButton("_", onCollapse)
                    BubbleTextButton("X", onDismiss)
                }
            }

            BubbleInputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { onLookup(query) },
            )

            BubbleResult(
                state = state,
                onRetry = { onLookup(query) },
                playback = playback,
            )
        }
    }
}

@Composable
private fun BubbleTextButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .clickable { onClick() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BubbleInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bubble_input"),
        singleLine = true,
        label = { Text("Word") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}

@Composable
private fun BubbleResult(
    state: LookupState,
    onRetry: () -> Unit,
    playback: PronunciationPlayback,
) {
    when (state) {
        LookupState.Idle -> BubbleHint("Type a word, then press search.")
        is LookupState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Looking up ${state.query}", style = MaterialTheme.typography.bodyMedium)
        }
        is LookupState.Content -> BubbleDefinition(
            entry = state.entries.firstOrNull(),
            playback = playback,
        )
        is LookupState.Suggestions -> Column {
            Text("Did you mean:", style = MaterialTheme.typography.bodyMedium)
            state.values.take(4).forEach { suggestion ->
                Text(
                    text = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        is LookupState.NoMatch -> BubbleHint("No match found for \"${state.query}\".")
        is LookupState.InvalidInput -> BubbleHint("Enter a valid English word.")
        is LookupState.Failure -> BubbleError(state.error, onRetry)
    }
}

@Composable
private fun BubbleDefinition(
    entry: DictionaryEntry?,
    playback: PronunciationPlayback,
) {
    if (entry == null) {
        BubbleHint("No readable definition.")
        return
    }
    val definition = entry.definitions.firstOrNull { it.text.isNotBlank() }?.text
        ?: entry.shortDefinition ?: run {
            BubbleHint("No readable definition.")
            return
        }
    val pronunciation = entry.pronunciations.firstOrNull()
    val audioUrl = MerriamWebsterAudio.urlFor(pronunciation?.audioReference)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = entry.headword,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        entry.functionalLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = definition,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (audioUrl != null) {
            BubblePronunciationButton(audioUrl, playback, entry.headword)
        }
        Text(
            text = "Merriam-Webster's Learner's Dictionary",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BubblePronunciationButton(
    audioUrl: String,
    playback: PronunciationPlayback,
    word: String,
) {
    val playbackState by playback.state.collectAsState()
    val label = when (playbackState) {
        AudioPlaybackState.Idle, AudioPlaybackState.Completed -> "Listen"
        AudioPlaybackState.Loading -> "Loading..."
        AudioPlaybackState.Playing -> "Playing..."
        is AudioPlaybackState.Error -> "Try again"
    }
    Text(
        text = label,
        modifier = Modifier
            .testTag("bubble_pronounce")
            .clickable { playback.play(audioUrl) },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BubbleHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BubbleError(error: DictionaryError, onRetry: () -> Unit) {
    val message = when (error) {
        is DictionaryError.MissingConfiguration -> "Dictionary setup needed."
        DictionaryError.InvalidCredential -> "Dictionary key isn't valid."
        DictionaryError.QuotaExceeded -> "Lookup limit reached."
        DictionaryError.Offline -> "You're offline."
        DictionaryError.Timeout -> "The lookup took too long."
        DictionaryError.NonJsonResponse -> "Unexpected service response."
        is DictionaryError.Server -> "Dictionary service unavailable."
        is DictionaryError.MalformedContent -> "We couldn't read this entry."
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Try again",
            modifier = Modifier
                .clickable { onRetry() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
