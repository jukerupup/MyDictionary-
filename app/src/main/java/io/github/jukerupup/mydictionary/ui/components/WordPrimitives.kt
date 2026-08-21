package io.github.jukerupup.mydictionary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.jukerupup.mydictionary.ui.theme.DictionaryShapes
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing
import io.github.jukerupup.mydictionary.ui.theme.DictionaryTheme

enum class PronunciationState {
    Available,
    Loading,
    Playing,
    Error,
    Unavailable,
}

@Composable
fun WordHeader(
    word: String,
    partOfSpeech: String?,
    ipa: String?,
    pronunciationState: PronunciationState,
    onPronounce: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("word_header"),
        verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
    ) {
        Text(
            text = word,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displayMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
            verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
        ) {
            partOfSpeech?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ipa?.let {
                Text(
                    text = it,
                    modifier = Modifier.semantics {
                        contentDescription = "Pronunciation notation: $it"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PronunciationControl(
                word = word,
                state = pronunciationState,
                onClick = onPronounce,
            )
        }
    }
}

@Composable
fun PronunciationControl(
    word: String,
    state: PronunciationState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == PronunciationState.Unavailable) {
        Text(
            text = "Audio not available",
            modifier = modifier.semantics {
                contentDescription = "Pronunciation audio is not available for $word"
            },
            style = MaterialTheme.typography.labelLarge,
            color = DictionaryTheme.colors.warning,
        )
        return
    }

    val label = when (state) {
        PronunciationState.Available -> "Listen"
        PronunciationState.Loading -> "Loading audio"
        PronunciationState.Playing -> "Playing"
        PronunciationState.Error -> "Try audio again"
        PronunciationState.Unavailable -> error("Handled above")
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .testTag("pronunciation_control")
            .semantics {
                role = Role.Button
                contentDescription = "$label for $word"
            },
        enabled = state != PronunciationState.Loading,
    ) {
        if (state == PronunciationState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.width(DictionarySpacing.Space5),
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(
                start = if (state == PronunciationState.Loading) {
                    DictionarySpacing.Space2
                } else {
                    DictionarySpacing.Space1
                },
            ),
        )
    }
}

@Composable
fun DefinitionBlock(
    definition: String,
    modifier: Modifier = Modifier,
    senseNumber: Int? = null,
) {
    LearnerMarginBlock(
        markerColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.testTag("definition_block"),
    ) {
        senseNumber?.let {
            Text(
                text = "Meaning $it",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = definition,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun ExampleBlock(
    example: String,
    modifier: Modifier = Modifier,
) {
    LearnerMarginBlock(
        markerColor = DictionaryTheme.colors.example,
        modifier = modifier
            .testTag("example_block")
            .semantics {
                contentDescription = "Example: $example"
            },
        background = DictionaryTheme.colors.exampleContainer,
    ) {
        Text(
            text = example,
            style = MaterialTheme.typography.bodyMedium,
            color = DictionaryTheme.colors.onExampleContainer,
        )
    }
}

@Composable
private fun LearnerMarginBlock(
    markerColor: Color,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(background)
            .padding(DictionarySpacing.Space3),
        horizontalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(DictionarySpacing.Space1)
                .background(markerColor, DictionaryShapes.Small),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(DictionarySpacing.Space1),
            verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
        ) {
            content()
        }
    }
}

@Composable
fun LearningChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    actionable: Boolean = true,
) {
    if (actionable) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        AssistChip(
            onClick = {},
            label = { Text(label) },
            modifier = modifier,
            enabled = false,
        )
    }
}
