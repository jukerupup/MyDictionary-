package io.github.jukerupup.mydictionary.ui.audio

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme

class AudioQaActivity : ComponentActivity() {
    fun showAudioSurface(audioController: AudioController) {
        setContent {
            AudioQaSurface(audioController)
        }
    }
}

@Composable
private fun AudioQaSurface(audioController: AudioController) {
    MyDictionaryTheme {
        val playback = rememberPronunciationPlayback(audioController)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(DictionarySpacing.Space4)
                .testTag("audio_qa_surface"),
            verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space4),
        ) {
            Text("resilient", style = MaterialTheme.typography.displayMedium)
            Text(DEFINITION, style = MaterialTheme.typography.bodyLarge)
            PronunciationControl(
                word = "resilient",
                audioUrl = AUDIO_URL,
                playback = playback,
            )
        }
    }
}

private const val AUDIO_URL =
    "https://media.merriam-webster.com/audio/prons/en/us/mp3/r/resili01.mp3"
private const val DEFINITION = "able to become strong, healthy, or successful again"
