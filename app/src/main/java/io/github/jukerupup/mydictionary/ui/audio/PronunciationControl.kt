package io.github.jukerupup.mydictionary.ui.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.ui.theme.DictionaryShapes
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing

@Composable
fun rememberPronunciationPlayback(audioController: AudioController): PronunciationPlayback {
    val scope = rememberCoroutineScope()
    val playback = remember(audioController) {
        PronunciationPlayback(audioController = audioController, scope = scope)
    }
    DisposableEffect(playback) {
        onDispose(playback::release)
    }
    return playback
}

@Composable
fun PronunciationControl(
    word: String,
    audioUrl: String?,
    playback: PronunciationPlayback,
    modifier: Modifier = Modifier,
) {
    val state by playback.state.collectAsState()
    if (audioUrl == null && state !is AudioPlaybackState.Error) {
        Text(
            text = "Audio not available",
            modifier = modifier
                .testTag("pronunciation_unavailable")
                .semantics {
                    contentDescription = "Pronunciation audio is not available for $word"
                },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val label = when (state) {
        AudioPlaybackState.Idle, AudioPlaybackState.Completed -> "Listen"
        AudioPlaybackState.Loading -> "Loading audio"
        AudioPlaybackState.Playing -> "Playing"
        is AudioPlaybackState.Error -> "Try audio again"
    }
    Button(
        onClick = { playback.play(audioUrl) },
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .testTag("pronunciation_control")
            .semantics {
                contentDescription = "$label for $word"
                liveRegion = LiveRegionMode.Polite
            },
        enabled = state != AudioPlaybackState.Loading,
        shape = DictionaryShapes.Small,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(DictionarySpacing.Space2)) {
            if (state == AudioPlaybackState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = SpeakerIcon,
                    contentDescription = null,
                )
            }
            Text(label)
        }
    }
}

private val SpeakerIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Speaker",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 9f)
            lineTo(7f, 9f)
            lineTo(12f, 4f)
            lineTo(12f, 20f)
            lineTo(7f, 15f)
            lineTo(3f, 15f)
            close()
            moveTo(16f, 8f)
            curveTo(17.3f, 9f, 18f, 10.3f, 18f, 12f)
            curveTo(18f, 13.7f, 17.3f, 15f, 16f, 16f)
            lineTo(14.8f, 14.4f)
            curveTo(15.6f, 13.8f, 16f, 13f, 16f, 12f)
            curveTo(16f, 11f, 15.6f, 10.2f, 14.8f, 9.6f)
            close()
        }
    }.build()
}
