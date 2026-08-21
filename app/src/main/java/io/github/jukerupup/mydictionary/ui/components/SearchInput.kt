package io.github.jukerupup.mydictionary.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing

@Composable
fun DictionarySearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorMessage: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
                .semantics {
                    if (errorMessage != null) error(errorMessage)
                },
            enabled = enabled,
            isError = errorMessage != null,
            label = { Text("Look up an English word") },
            supportingText = errorMessage?.let { message ->
                { Text(message) }
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    TextButton(
                        onClick = { onQueryChange("") },
                        enabled = enabled,
                    ) {
                        Text("Clear")
                    }
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        if (!enabled) {
            Text(
                text = "Search is unavailable while setup is incomplete.",
                modifier = Modifier.padding(top = DictionarySpacing.Space1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
