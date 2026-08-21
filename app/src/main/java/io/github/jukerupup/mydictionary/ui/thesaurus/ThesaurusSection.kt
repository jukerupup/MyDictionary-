package io.github.jukerupup.mydictionary.ui.thesaurus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.jukerupup.mydictionary.domain.thesaurus.ThesaurusContent
import io.github.jukerupup.mydictionary.domain.thesaurus.ThesaurusExpansionState
import io.github.jukerupup.mydictionary.domain.thesaurus.ThesaurusLoadState
import io.github.jukerupup.mydictionary.ui.components.ExpansionRow
import io.github.jukerupup.mydictionary.ui.components.StatusKind
import io.github.jukerupup.mydictionary.ui.components.StatusPanel
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing
import io.github.jukerupup.mydictionary.ui.theme.DictionaryTheme

@Composable
fun ThesaurusSection(
    state: ThesaurusExpansionState,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("thesaurus_section"),
        verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
    ) {
        ExpansionRow(
            title = "Related vocabulary",
            expanded = state.expanded,
            onToggle = onToggle,
            loading = false,
            enabled = state.headword.isNotEmpty(),
            modifier = Modifier.testTag("thesaurus_toggle"),
        )
        if (state.expanded) {
            when (val content = state.content) {
                ThesaurusLoadState.Idle -> Unit
                ThesaurusLoadState.Loading -> StatusPanel(
                    kind = StatusKind.Loading,
                    title = "Finding related words",
                    message = "Looking up thesaurus entries for ${state.headword}.",
                    modifier = Modifier.testTag("thesaurus_loading"),
                )
                is ThesaurusLoadState.Error -> StatusPanel(
                    kind = StatusKind.PartialSuccess,
                    title = content.title,
                    message = content.message,
                    actionLabel = "Try again",
                    onAction = onRetry,
                    modifier = Modifier.testTag("thesaurus_error"),
                )
                is ThesaurusLoadState.Ready -> ThesaurusResult(content.value)
            }
        }
    }
}

@Composable
private fun ThesaurusResult(content: ThesaurusContent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DictionarySpacing.Space4)
            .testTag("thesaurus_result"),
        verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space4),
    ) {
        content.meaning?.let { Meaning(it) }
        VocabularyGroup("Synonyms", content.synonyms, "thesaurus_synonyms")
        VocabularyGroup("Related words", content.relatedWords, "thesaurus_related_words")
        VocabularyGroup("Near-antonyms", content.nearAntonyms, "thesaurus_near_antonyms")
        VocabularyGroup("Antonyms", content.antonyms, "thesaurus_antonyms")
        VocabularyGroup("Phrases", content.phrases, "thesaurus_phrases")
        VocabularyGroup("Examples", content.examples, "thesaurus_examples")
    }
}

@Composable
private fun Meaning(value: String) {
    Column(
        modifier = Modifier.testTag("thesaurus_meaning"),
        verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
    ) {
        GroupHeading("Shared meaning")
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun VocabularyGroup(title: String, values: List<String>, tag: String) {
    if (values.isEmpty()) return
    Column(
        modifier = Modifier.testTag(tag),
        verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        GroupHeading(title)
        values.forEach { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (title == "Examples") DictionaryTheme.colors.example
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GroupHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() },
    )
}
