package io.github.jukerupup.mydictionary.ui.audio

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jukerupup.mydictionary.audio.Media3AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PronunciationControlInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<AudioQaActivity>()

    @Test
    fun visibleStatesDebounceRecoveryAndLifecycleRelease() {
        val fakeAudio = FakeAudioController()
        composeRule.runOnUiThread {
            composeRule.activity.showAudioSurface(fakeAudio)
        }

        composeRule.onNodeWithTag("pronunciation_control").performClick().performClick()
        assertEquals(1, fakeAudio.playCount.get())
        composeRule.onNodeWithText("Loading audio").assertIsDisplayed()
        capture("loading.png")

        fakeAudio.emit(AudioPlaybackState.Playing)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Playing").assertIsDisplayed()
        capture("playing.png")

        fakeAudio.emit(AudioPlaybackState.Error("Pronunciation audio could not be played"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Try audio again").assertIsDisplayed()
        composeRule.onNodeWithText(DEFINITION).assertIsDisplayed()
        capture("error.png")

        composeRule.onNodeWithTag("pronunciation_control").performClick()
        assertEquals(2, fakeAudio.playCount.get())
        assertEquals(1, fakeAudio.activePlaybackCount.get())

        composeRule.activityRule.scenario.close()
        assertEquals(1, fakeAudio.releaseCount.get())
        assertEquals(0, fakeAudio.activePlaybackCount.get())

        appendTrace(
            "surface loading=visible playing=visible error=recoverable " +
                "rapidPlayCount=1 retryPlayCount=2 releaseCount=1 activeAfterDestroy=0",
        )
    }

    @Test
    fun unreachableUrlProducesBoundedNonfatalMedia3Error() {
        lateinit var controller: Media3AudioController
        composeRule.runOnUiThread {
            controller = Media3AudioController(composeRule.activity)
            controller.play("https://127.0.0.1:1/unreachable.mp3")
        }

        composeRule.waitUntil(timeoutMillis = 15_000L) {
            controller.state.value is AudioPlaybackState.Error
        }
        assertTrue(controller.state.value is AudioPlaybackState.Error)
        composeRule.runOnUiThread(controller::release)
        assertEquals(AudioPlaybackState.Idle, controller.state.value)
        appendTrace("unreachableUrl=nonfatal_error media3Released=true")
    }

    private fun capture(name: String) {
        composeRule.runOnUiThread {
            composeRule.activity.window.decorView.invalidate()
        }
        composeRule.waitForIdle()
        Thread.sleep(300L)
        val bitmap = requireNotNull(
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
        )
        val file = File(evidenceDirectory(), name)
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }

    private fun appendTrace(line: String) {
        File(evidenceDirectory(), "state-trace.txt").appendText("$line\n")
    }

    private fun evidenceDirectory(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(requireNotNull(context.getExternalFilesDir(null)), "task7").apply { mkdirs() }
    }

    private class FakeAudioController : AudioController {
        override val state = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
        val playCount = AtomicInteger()
        val activePlaybackCount = AtomicInteger()
        val releaseCount = AtomicInteger()

        override fun play(url: String) {
            playCount.incrementAndGet()
            activePlaybackCount.set(1)
            state.value = AudioPlaybackState.Loading
        }

        override fun stop() {
            activePlaybackCount.set(0)
            state.value = AudioPlaybackState.Idle
        }

        override fun release() {
            releaseCount.incrementAndGet()
            activePlaybackCount.set(0)
            state.value = AudioPlaybackState.Idle
        }

        fun emit(newState: AudioPlaybackState) {
            state.value = newState
        }
    }

}

private const val DEFINITION = "able to become strong, healthy, or successful again"
