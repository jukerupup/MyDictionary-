package io.github.jukerupup.mydictionary.ui.audio

import android.os.SystemClock
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import java.net.URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PronunciationPlayback(
    private val audioController: AudioController,
    scope: CoroutineScope,
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) {
    private val mutableState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val state: StateFlow<AudioPlaybackState> = mutableState.asStateFlow()

    private var currentUrl: String? = null
    private var lastPlayMillis = Long.MIN_VALUE
    private var released = false
    private var acceptControllerState = true
    private val stateCollection: Job = scope.launch {
        audioController.state.collect { state ->
            if (acceptControllerState) mutableState.emit(state)
        }
    }

    fun play(url: String?) {
        if (released) return
        val playableUrl = url?.takeIf(::isOfficialPronunciationUrl)
        if (playableUrl == null) {
            if (currentUrl != null) audioController.stop()
            currentUrl = null
            acceptControllerState = false
            mutableState.value = AudioPlaybackState.Error("Pronunciation audio is unavailable")
            return
        }

        val now = elapsedRealtimeMillis()
        if (
            playableUrl == currentUrl &&
            now - lastPlayMillis in 0 until DOUBLE_TAP_WINDOW_MILLIS &&
            mutableState.value.isActive
        ) {
            return
        }

        if (currentUrl != null) audioController.stop()
        currentUrl = playableUrl
        lastPlayMillis = now
        acceptControllerState = true
        mutableState.value = AudioPlaybackState.Loading
        audioController.play(playableUrl)
    }

    fun stop() {
        if (released) return
        audioController.stop()
        currentUrl = null
        acceptControllerState = true
        mutableState.value = AudioPlaybackState.Idle
    }

    fun release() {
        if (released) return
        released = true
        stateCollection.cancel()
        audioController.release()
        currentUrl = null
        mutableState.value = AudioPlaybackState.Idle
    }

    private fun isOfficialPronunciationUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" &&
            uri.host.equals(OFFICIAL_AUDIO_HOST, ignoreCase = true) &&
            uri.port == -1 &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null &&
            uri.path.startsWith(OFFICIAL_AUDIO_PATH) &&
            uri.path.endsWith(".mp3")
    }.getOrDefault(false)

    private val AudioPlaybackState.isActive: Boolean
        get() = this == AudioPlaybackState.Loading || this == AudioPlaybackState.Playing

    private companion object {
        const val DOUBLE_TAP_WINDOW_MILLIS = 300L
        const val OFFICIAL_AUDIO_HOST = "media.merriam-webster.com"
        const val OFFICIAL_AUDIO_PATH = "/audio/prons/en/us/mp3/"
    }
}
