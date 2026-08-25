package io.github.jukerupup.mydictionary

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.res.Configuration
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.jukerupup.mydictionary.bubble.BubbleService
import io.github.jukerupup.mydictionary.ui.lookup.LookupScreen
import io.github.jukerupup.mydictionary.ui.lookup.LookupViewModel
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // After returning from settings, start the bubble if permission is granted.
        if (Settings.canDrawOverlays(this)) {
            BubbleService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDarkTheme = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
        val container = (application as MyDictionaryApplication).container
        val audioController = container.audioControllerFactory(this)
        val viewModel = ViewModelProvider(
            this,
            LookupViewModel.Factory(container.dictionaryRepository, audioController),
        )[LookupViewModel::class.java]
        setContent {
            MyDictionaryTheme {
                val state by viewModel.state.collectAsState()
                val thesaurusState by viewModel.thesaurusState.collectAsState()
                LookupScreen(
                    state = state,
                    configuration = container.configuration,
                    onLookup = viewModel::lookup,
                    onPronounce = viewModel::requestPronunciation,
                    thesaurusState = thesaurusState,
                    onToggleThesaurus = viewModel::toggleThesaurus,
                    onRetryThesaurus = viewModel::retryThesaurus,
                    onStartBubble = ::requestBubble,
                )
            }
        }
    }

    private fun requestBubble() {
        if (Settings.canDrawOverlays(this)) {
            BubbleService.start(this)
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
}
