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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                .then(
                    if (scrollable) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                )
                .padding(DictionarySpacing.Space5),
            verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
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
            Text(
                text = "Definition content from Merriam-Webster Inc.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
                    .widthIn(max = DictionaryLayout.MaxDialogWidth)
                    .heightIn(max = maxHeight - DictionarySpacing.Space8),
            )
        }
    }
}
