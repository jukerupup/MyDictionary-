package io.github.jukerupup.mydictionary.bubble

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import io.github.jukerupup.mydictionary.domain.audio.AudioController
import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.model.Definition
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.Pronunciation
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BubbleOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `bubble starts collapsed and expands on tap`() {
        val expandedEvents = mutableListOf<Boolean>()
        composeRule.setContent {
            BubbleOverlay(
                controller = controller(RecordingRepository()),
                audioController = FakeAudioController(),
                onDismiss = {},
                onExpandedChanged = { expandedEvents += it },
            )
        }

        composeRule.onNodeWithTag("bubble_ball").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_card").assertDoesNotExist()

        composeRule.onNodeWithTag("bubble_ball").performClick()

        composeRule.onNodeWithTag("bubble_card").assertIsDisplayed()
        assertEquals(listOf(true), expandedEvents)
    }

    @Test
    fun `typing a word and searching shows the learner definition`() {
        composeRule.setContent {
            BubbleOverlay(
                controller = controller(RecordingRepository()),
                audioController = FakeAudioController(),
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag("bubble_ball").performClick()

        composeRule.onNodeWithTag("bubble_input").performTextInput("resilient")
        composeRule.onNodeWithTag("bubble_input").performImeAction()

        composeRule.onNodeWithText("able to become strong after difficulty").assertIsDisplayed()
    }

    @Test
    fun `pronunciation button is present with audio reference`() {
        composeRule.setContent {
            BubbleOverlay(
                controller = controller(RecordingRepository()),
                audioController = FakeAudioController(),
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag("bubble_ball").performClick()
        composeRule.onNodeWithTag("bubble_input").performTextInput("resilient")
        composeRule.onNodeWithTag("bubble_input").performImeAction()

        composeRule.onNodeWithTag("bubble_pronounce").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_pronounce").assertTextContains("Listen")
    }

    @Test
    fun `collapse returns to ball and fires focus event`() {
        val expandedEvents = mutableListOf<Boolean>()
        composeRule.setContent {
            BubbleOverlay(
                controller = controller(RecordingRepository()),
                audioController = FakeAudioController(),
                onDismiss = {},
                onExpandedChanged = { expandedEvents += it },
            )
        }
        composeRule.onNodeWithTag("bubble_ball").performClick()
        composeRule.onNodeWithTag("bubble_card").assertIsDisplayed()

        composeRule.onNodeWithText("_").performClick()

        composeRule.onNodeWithTag("bubble_ball").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_card").assertDoesNotExist()
        assertTrue(expandedEvents.contains(false))
    }

    @Test
    fun `dismiss invokes onDismiss callback`() {
        var dismissed = false
        composeRule.setContent {
            BubbleOverlay(
                controller = controller(RecordingRepository()),
                audioController = FakeAudioController(),
                onDismiss = { dismissed = true },
            )
        }
        composeRule.onNodeWithTag("bubble_ball").performClick()
        composeRule.onNodeWithText("X").performClick()

        assertTrue(dismissed)
    }

    @Test
    fun `open full page fires callback with headword and collapses bubble`() {
        val openedWords = mutableListOf<String>()
        val expandedEvents = mutableListOf<Boolean>()
        composeRule.setContent {
            BubbleOverlay(
                controller = controller(RecordingRepository()),
                audioController = FakeAudioController(),
                onDismiss = {},
                onExpandedChanged = { expandedEvents += it },
                onOpenWeb = { openedWords += it },
            )
        }
        composeRule.onNodeWithTag("bubble_ball").performClick()
        composeRule.onNodeWithTag("bubble_input").performTextInput("resilient")
        composeRule.onNodeWithTag("bubble_input").performImeAction()

        composeRule.onNodeWithTag("bubble_open_web").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_open_web").performClick()

        assertEquals(listOf("resilient"), openedWords)
        composeRule.onNodeWithTag("bubble_ball").assertIsDisplayed()
        assertTrue(expandedEvents.contains(false))
    }

    @Test
    fun `no match state shows a hint`() {
        val repository = object : DictionaryRepository {
            override suspend fun lookupDefinition(query: String) = LookupResult.NoMatch
            override suspend fun lookupThesaurus(query: String) = LookupResult.NoMatch
        }
        composeRule.setContent {
            BubbleOverlay(
                controller = controller(repository),
                audioController = FakeAudioController(),
                onDismiss = {},
            )
        }
        composeRule.onNodeWithTag("bubble_ball").performClick()
        composeRule.onNodeWithTag("bubble_input").performTextInput("zzzz")
        composeRule.onNodeWithTag("bubble_input").performImeAction()

        composeRule.onNodeWithText("No match found for \"zzzz\".").assertIsDisplayed()
    }

    private fun controller(repository: DictionaryRepository): BubbleController =
        BubbleController(
            repository = repository,
            scope = scope,
            dispatcher = Dispatchers.Unconfined,
        )

    private class RecordingRepository : DictionaryRepository {
        override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> =
            LookupResult.Success(
                listOf(
                    DictionaryEntry(
                        headword = query,
                        functionalLabel = "adjective",
                        pronunciations = listOf(Pronunciation("/rɪˈzɪljənt/", "resili03")),
                        definitions = listOf(
                            Definition(
                                "able to become strong after difficulty",
                                listOf("She remained resilient after the setback."),
                            ),
                        ),
                        offensive = false,
                    ),
                ),
            )

        override suspend fun lookupThesaurus(query: String) = LookupResult.NoMatch
    }

    private class FakeAudioController : AudioController {
        override val state = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)

        override fun play(url: String) {
            state.value = AudioPlaybackState.Loading
        }

        override fun stop() {
            state.value = AudioPlaybackState.Idle
        }

        override fun release() {
            state.value = AudioPlaybackState.Idle
        }
    }
}
