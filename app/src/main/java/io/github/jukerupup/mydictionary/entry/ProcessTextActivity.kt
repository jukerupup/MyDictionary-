package io.github.jukerupup.mydictionary.entry

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelProvider
import io.github.jukerupup.mydictionary.MyDictionaryApplication
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.ui.components.QuickDefineCard
import io.github.jukerupup.mydictionary.ui.theme.DictionaryLayout
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme

class ProcessTextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)

        val repository = repositoryOverride
            ?: (application as MyDictionaryApplication).container.dictionaryRepository
        val viewModel = ViewModelProvider(
            this,
            ProcessTextViewModel.Factory(repository),
        )[ProcessTextViewModel::class.java]
        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        viewModel.start(
            ProcessTextRequest(
                text = selectedText,
                readOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false),
            ),
        )

        setContent {
            MyDictionaryTheme {
                val state by viewModel.state.collectAsState()
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(DictionarySpacing.Space4)
                        .testTag("process_text_dialog"),
                    contentAlignment = Alignment.Center,
                ) {
                    QuickDefineCard(
                        state = state,
                        selectedText = selectedText?.toString(),
                        onDismiss = {
                            viewModel.dismiss()
                            finish()
                        },
                        onPronounce = pronunciationCallback,
                        modifier = Modifier
                            .widthIn(max = DictionaryLayout.MaxDialogWidth)
                            .heightIn(max = maxHeight - DictionarySpacing.Space8),
                    )
                }
            }
        }
    }

    internal companion object {
        @Volatile
        var repositoryOverride: DictionaryRepository? = null

        val pronunciationCallback: (String) -> Unit = {}
    }
}
