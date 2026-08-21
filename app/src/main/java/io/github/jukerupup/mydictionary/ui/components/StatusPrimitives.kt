package io.github.jukerupup.mydictionary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.jukerupup.mydictionary.ui.theme.DictionaryShapes
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing
import io.github.jukerupup.mydictionary.ui.theme.DictionaryTheme

enum class StatusKind {
    Loading,
    Empty,
    Suggestion,
    Configuration,
    Offline,
    Error,
    PartialSuccess,
}

@Composable
fun StatusPanel(
    kind: StatusKind,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val isError = kind == StatusKind.Error || kind == StatusKind.Offline
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_${kind.name.lowercase()}")
            .semantics {
                liveRegion = LiveRegionMode.Polite
                stateDescription = title
            },
        shape = DictionaryShapes.Medium,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(DictionarySpacing.Space4),
            horizontalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
            verticalAlignment = Alignment.Top,
        ) {
            if (kind == StatusKind.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = DictionarySpacing.Space1),
                    strokeWidth = 2.dp,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(message, style = MaterialTheme.typography.bodyMedium)
                actionLabel?.let {
                    OutlinedButton(onClick = onAction) {
                        Text(it)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpansionRow(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val stateText = when {
        loading -> "Loading"
        expanded -> "Expanded"
        else -> "Collapsed"
    }
    Surface(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .testTag("expansion_row")
            .semantics {
                role = Role.Button
                stateDescription = stateText
                contentDescription = "$title, $stateText"
            },
        enabled = enabled && !loading,
        shape = DictionaryShapes.Medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(DictionarySpacing.Space4),
            horizontalArrangement = Arrangement.spacedBy(DictionarySpacing.Space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = when {
                    loading -> "Loading"
                    expanded -> "Hide"
                    else -> "Show"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    DictionaryTheme.colors.disabledContent
                },
            )
        }
    }
}
