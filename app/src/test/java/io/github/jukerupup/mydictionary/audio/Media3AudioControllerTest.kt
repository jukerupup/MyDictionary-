package io.github.jukerupup.mydictionary.audio

import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Media3AudioControllerTest {
    private val engine = FakePronunciationPlayerEngine()
    private val controller = Media3AudioController(engine)

    @Test
    fun playbackPublishesLoadingPlayingCompletionAndError() {
        controller.play(FIRST_URL)
        val request = engine.currentRequestId
        assertEquals(AudioPlaybackState.Loading, controller.state.value)
        engine.emit(request, PlayerEvent.Playing)
        assertEquals(AudioPlaybackState.Playing, controller.state.value)
        engine.emit(request, PlayerEvent.Completed)
        assertEquals(AudioPlaybackState.Completed, controller.state.value)

        controller.play(FIRST_URL)
        engine.emit(engine.currentRequestId, PlayerEvent.Error)
        assertTrue(controller.state.value is AudioPlaybackState.Error)
    }

    @Test
    fun replacementKeepsOneActivePlaybackAndIgnoresLatePriorCallback() {
        controller.play(FIRST_URL)
        val replacedRequest = engine.currentRequestId
        controller.play(SECOND_URL)
        val activeRequest = engine.currentRequestId

        assertEquals(1, engine.activePlaybackCount)
        assertEquals(SECOND_URL, engine.currentUrl)
        engine.emit(replacedRequest, PlayerEvent.Completed)
        assertEquals(AudioPlaybackState.Loading, controller.state.value)
        engine.emit(activeRequest, PlayerEvent.Playing)
        assertEquals(AudioPlaybackState.Playing, controller.state.value)
    }

    @Test
    fun stopAndReleasePreventLateEventsAndReleaseExactlyOnce() {
        controller.play(FIRST_URL)
        val stoppedRequest = engine.currentRequestId
        controller.stop()
        engine.emit(stoppedRequest, PlayerEvent.Playing)
        assertEquals(AudioPlaybackState.Idle, controller.state.value)
        assertEquals(0, engine.activePlaybackCount)

        controller.release()
        controller.release()
        engine.emit(stoppedRequest, PlayerEvent.Error)
        assertEquals(AudioPlaybackState.Idle, controller.state.value)
        assertEquals(1, engine.releaseCount)
    }

    private class FakePronunciationPlayerEngine : PronunciationPlayerEngine {
        private var listener: (Long, PlayerEvent) -> Unit = { _, _ -> }
        var currentRequestId = 0L
        var currentUrl: String? = null
        var activePlaybackCount = 0
        var releaseCount = 0

        override fun setListener(listener: (requestId: Long, event: PlayerEvent) -> Unit) {
            this.listener = listener
        }

        override fun play(url: String, requestId: Long) {
            currentUrl = url
            currentRequestId = requestId
            activePlaybackCount = 1
        }

        override fun stop() {
            currentUrl = null
            activePlaybackCount = 0
        }

        override fun release() {
            currentUrl = null
            activePlaybackCount = 0
            releaseCount += 1
        }

        fun emit(requestId: Long, event: PlayerEvent) {
            listener(requestId, event)
        }
    }

    private companion object {
        const val FIRST_URL =
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/r/resili01.mp3"
        const val SECOND_URL =
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/a/alpha001.mp3"
    }
}
