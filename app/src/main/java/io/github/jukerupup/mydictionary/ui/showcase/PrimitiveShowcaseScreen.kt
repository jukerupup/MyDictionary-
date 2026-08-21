package io.github.jukerupup.mydictionary.ui.showcase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.jukerupup.mydictionary.ui.components.DefinitionBlock
import io.github.jukerupup.mydictionary.ui.components.DictionarySearchInput
import io.github.jukerupup.mydictionary.ui.components.ExampleBlock
import io.github.jukerupup.mydictionary.ui.components.ExpansionRow
import io.github.jukerupup.mydictionary.ui.components.LearningChip
import io.github.jukerupup.mydictionary.ui.components.PronunciationState
import io.github.jukerupup.mydictionary.ui.components.QuickDefineCard
import io.github.jukerupup.mydictionary.ui.components.QuickDefineDialog
import io.github.jukerupup.mydictionary.ui.components.StatusKind
import io.github.jukerupup.mydictionary.ui.components.StatusPanel
import io.github.jukerupup.mydictionary.ui.components.WordHeader
import io.github.jukerupup.mydictionary.ui.theme.DictionaryLayout
import io.github.jukerupup.mydictionary.ui.theme.DictionaryShapes
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing

private data class ShowcaseStatus(
    val kind: StatusKind,
    val title: String,
    val message: String,
    val action: String? = null,
)

private val ShowcaseStatuses = listOf(
    ShowcaseStatus(StatusKind.Loading, "Looking up resilient", "Finding a learner-friendly definition."),
    ShowcaseStatus(StatusKind.Empty, "No exact match", "Check the spelling or choose a suggestion below."),
    ShowcaseStatus(StatusKind.Suggestion, "Did you mean resilient?", "Suggestions start a new lookup."),
    ShowcaseStatus(StatusKind.Configuration, "Dictionary setup needed", "Add the Learner's Dictionary key in local.properties."),
    ShowcaseStatus(StatusKind.Offline, "You're offline", "Reconnect, then try this word again.", "Try again"),
    ShowcaseStatus(StatusKind.Error, "Definition unavailable", "The service returned content this version cannot read.", "Try again"),
    ShowcaseStatus(StatusKind.PartialSuccess, "Related words unavailable", "The definition above is still ready to use.", "Try again"),
)

@Composable
fun PrimitiveShowcaseScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("resilient") }
    var expanded by remember { mutableStateOf(false) }
    var dialogAudioAvailable by remember { mutableStateOf<Boolean?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("primitive_showcase"),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = DictionaryLayout.MaxReadingWidth)
                .testTag("showcase_list"),
            contentPadding = PaddingValues(
                horizontal = DictionarySpacing.Space4,
                vertical = DictionarySpacing.Space6,
            ),
            verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space6),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2)) {
                    Text(
                        text = "MyDictionary",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Learner-first primitive showcase",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                ShowcaseSection(title = "Search input", tag = "showcase_search") {
                    DictionarySearchInput(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                    )
                    DictionarySearchInput(
                        query = "pneumonoultramicroscopicsilicovolcanoconiosis",
                        onQueryChange = {},
                        onSearch = {},
                        errorMessage = "Use no more than 80 characters.",
                    )
                    DictionarySearchInput(
                        query = "audio",
                        onQueryChange = {},
                        onSearch = {},
                        enabled = false,
                    )
                }
            }
            item {
                ShowcaseSection(title = "Word, sound, and meaning", tag = "showcase_word") {
                    WordHeader(
                        word = "resilient",
                        partOfSpeech = "adjective",
                        ipa = "/rɪˈzɪliənt/",
                        pronunciationState = PronunciationState.Available,
                        onPronounce = {},
                    )
                    DefinitionBlock(
                        definition = "able to become strong, healthy, or successful again after something difficult happens",
                        senseNumber = 1,
                    )
                    ExampleBlock("She remained resilient after the setback.")
                }
            }
            item {
                ShowcaseSection(title = "Quick Define dialog", tag = "showcase_dialog_launchers") {
                    Text(
                        text = "A focused overlay keeps the learner in the app they are reading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { dialogAudioAvailable = true },
                        modifier = Modifier.testTag("open_quick_define_dialog"),
                    ) {
                        Text("Open Quick Define")
                    }
                    OutlinedButton(
                        onClick = { dialogAudioAvailable = false },
                        modifier = Modifier.testTag("open_missing_audio_dialog"),
                    ) {
                        Text("Preview missing audio")
                    }
                }
            }
            item {
                ShowcaseSection(title = "State laboratory", tag = "showcase_word_states") {
                    WordHeader(
                        word = "a word with no recorded pronunciation",
                        partOfSpeech = "phrase",
                        ipa = null,
                        pronunciationState = PronunciationState.Unavailable,
                        onPronounce = {},
                    )
                    Text(
                        text = "No example is available for this meaning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                ShowcaseSection(title = "Suggestions and learning chips", tag = "showcase_chips") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
                        verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
                    ) {
                        LearningChip("resilient", onClick = {}, selected = true)
                        LearningChip("resilience", onClick = {})
                        LearningChip("resiliently", onClick = {})
                        LearningChip("unavailable", onClick = {}, enabled = false)
                        LearningChip("strong", onClick = {}, actionable = false)
                    }
                }
            }
            items(ShowcaseStatuses) { state ->
                StatusPanel(
                    kind = state.kind,
                    title = state.title,
                    message = state.message,
                    actionLabel = state.action,
                )
            }
            item {
                ShowcaseSection(title = "Progressive disclosure", tag = "showcase_expansion") {
                    ExpansionRow(
                        title = "Related words",
                        expanded = expanded,
                        onToggle = { expanded = !expanded },
                    )
                    ExpansionRow(
                        title = "More meanings",
                        expanded = false,
                        onToggle = {},
                        loading = true,
                    )
                }
            }
            item {
                ShowcaseSection(title = "Quick Define card states", tag = "showcase_dialog") {
                    QuickDefineCard(
                        word = "resilient",
                        definition = "able to become strong again after something difficult",
                        example = "She remained resilient after the setback.",
                    )
                    QuickDefineCard(
                        word = "counterrevolutionaries",
                        definition = "people who oppose a revolution and try to restore an earlier system of government",
                        audioAvailable = false,
                    )
                }
            }
        }

        dialogAudioAvailable?.let { audioAvailable ->
            QuickDefineDialog(
                word = if (audioAvailable) "resilient" else "counterrevolutionaries",
                definition = if (audioAvailable) {
                    "able to become strong again after something difficult"
                } else {
                    "people who oppose a revolution and try to restore an earlier system of government"
                },
                example = if (audioAvailable) "She remained resilient after the setback." else null,
                audioAvailable = audioAvailable,
                onDismiss = { dialogAudioAvailable = null },
            )
        }
    }
}

@Composable
private fun ShowcaseSection(
    title: String,
    tag: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = DictionaryShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(DictionarySpacing.Space4),
            verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space4),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}
