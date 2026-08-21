package io.github.jukerupup.mydictionary

import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.jukerupup.mydictionary.ui.lookup.LookupScreen
import io.github.jukerupup.mydictionary.ui.lookup.LookupViewModel
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDarkTheme = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
        val container = (application as MyDictionaryApplication).container
        val viewModel = ViewModelProvider(
            this,
            LookupViewModel.Factory(container.dictionaryRepository),
        )[LookupViewModel::class.java]
        setContent {
            MyDictionaryTheme {
                val state by viewModel.state.collectAsState()
                LookupScreen(
                    state = state,
                    configuration = container.configuration,
                    onLookup = viewModel::lookup,
                    onPronounce = viewModel::requestPronunciation,
                )
            }
        }
    }
}
