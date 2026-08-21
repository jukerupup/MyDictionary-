package io.github.jukerupup.mydictionary.ui.audio

import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PronunciationPlaybackTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val fakeAudio = FakeAudioController()
    private var elapsedMillis = 1_000L
    private val playback = PronunciationPlayback(
        audioController = fakeAudio,
        scope = scope,
        elapsedRealtimeMillis = { elapsedMillis },
    )

    @Test
    fun validAudioTransitionsThroughLoadingPlayingAndCompletion() {
        playback.play(VALID_URL)
        assertEquals(AudioPlaybackState.Loading, playback.state.value)

        fakeAudio.emit(AudioPlaybackState.Playing)
        scope.runCurrent()
        assertEquals(AudioPlaybackState.Playing, playback.state.value)

        fakeAudio.emit(AudioPlaybackState.Completed)
        scope.runCurrent()
        assertEquals(AudioPlaybackState.Completed, playback.state.value)
    }

    @Test
    fun missingOrMalformedUrlIsRecoverableAndDoesNotStartPlayer() {
        listOf(null, "", "not a url", "http://media.merriam-webster.com/audio.mp3").forEach {
            playback.play(it)
            assertTrue(playback.state.value is AudioPlaybackState.Error)
        }
        assertTrue(fakeAudio.playedUrls.isEmpty())

        playback.play(VALID_URL)
        assertEquals(AudioPlaybackState.Loading, playback.state.value)
        assertEquals(listOf(VALID_URL), fakeAudio.playedUrls)
    }

    @Test
    fun malformedReplacementStopsPriorAudioAndRetainsErrorState() {
        playback.play(VALID_URL)
        playback.play("javascript:alert(1)")
        scope.runCurrent()

        assertEquals(1, fakeAudio.stopCount)
        assertTrue(playback.state.value is AudioPlaybackState.Error)
    }

    @Test
    fun rapidRepeatIsDebouncedButDifferentAudioReplacesCurrentPlayback() {
        playback.play(VALID_URL)
        playback.play(VALID_URL)
        playback.play(SECOND_URL)

        assertEquals(listOf(VALID_URL, SECOND_URL), fakeAudio.playedUrls)
        assertEquals(1, fakeAudio.stopCount)
    }

    @Test
    fun laterRepeatRestartsAfterDebounceWindow() {
        playback.play(VALID_URL)
        elapsedMillis += 500L
        playback.play(VALID_URL)

        assertEquals(listOf(VALID_URL, VALID_URL), fakeAudio.playedUrls)
        assertEquals(1, fakeAudio.stopCount)
    }

    @Test
    fun playerFailureKeepsPlaybackRecoverable() {
        playback.play(VALID_URL)
        fakeAudio.emit(AudioPlaybackState.Error("Audio could not be played"))
        scope.runCurrent()
        assertTrue(playback.state.value is AudioPlaybackState.Error)

        elapsedMillis += 500L
        playback.play(VALID_URL)
        assertEquals(AudioPlaybackState.Loading, playback.state.value)
        assertEquals(2, fakeAudio.playedUrls.size)
    }

    @Test
    fun dismissStopsAndDestroyReleasesExactlyOnce() {
        playback.play(VALID_URL)
        playback.stop()
        playback.release()
        playback.release()

        assertEquals(AudioPlaybackState.Idle, playback.state.value)
        assertEquals(1, fakeAudio.stopCount)
        assertEquals(1, fakeAudio.releaseCount)
    }

    private class FakeAudioController : AudioController {
        override val state = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
        val playedUrls = mutableListOf<String>()
        var stopCount = 0
        var releaseCount = 0

        override fun play(url: String) {
            playedUrls += url
            state.value = AudioPlaybackState.Loading
        }

        override fun stop() {
            stopCount += 1
            state.value = AudioPlaybackState.Idle
        }

        override fun release() {
            releaseCount += 1
            state.value = AudioPlaybackState.Idle
        }

        fun emit(newState: AudioPlaybackState) {
            state.value = newState
        }
    }

    private companion object {
        const val VALID_URL =
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/r/resili01.mp3"
        const val SECOND_URL =
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/a/alpha001.mp3"
    }
}
