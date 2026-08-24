package io.github.jukerupup.mydictionary.entry

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jukerupup.mydictionary.domain.model.Definition
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.Pronunciation
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessTextActivityTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var repository: RecordingRepository

    @Before
    fun setUp() {
        repository = RecordingRepository()
        ProcessTextActivity.repositoryOverride = repository
    }

    @After
    fun tearDown() {
        ProcessTextActivity.repositoryOverride = null
    }

    @Test
    fun readOnlyProcessTextShowsSharedLearnerResultAndReturnsNoReplacement() {
        val scenario = ActivityScenario.launchActivityForResult<ProcessTextActivity>(
            processTextIntent("resilient", readOnly = true),
        )
        composeRule.waitUntil(5_000) { repository.requests.get() == 1 }

        composeRule.onNodeWithTag("process_text_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Define").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Define").assertIsFocused()
        composeRule.onNodeWithText("resilient").assertIsDisplayed()
        composeRule.onNodeWithText("able to become strong after difficulty").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Listen for resilient").performClick()
        composeRule.onNodeWithContentDescription("Try audio again for resilient").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()

        assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        assertEquals(1, repository.requests.get())
    }

    @Test
    fun recreationAndBackgroundForegroundDoNotDuplicateTheLookup() {
        ActivityScenario.launch<ProcessTextActivity>(processTextIntent("resilient")).use { scenario ->
            composeRule.waitUntil(5_000) { repository.requests.get() == 1 }
            scenario.recreate()
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            composeRule.onNodeWithText("able to become strong after difficulty").assertIsDisplayed()
            assertEquals(1, repository.requests.get())
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitUntil(5_000) {
                scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED
            }
        }
    }

    @Test
    fun missingAndOverlongExtrasShowDismissibleErrorsWithoutLookup() {
        listOf(null, "a".repeat(81)).forEach { value ->
            ActivityScenario.launch<ProcessTextActivity>(processTextIntent(value)).use {
                val expected = if (value == null) "No word selected" else "Selection is too long"
                composeRule.onNodeWithText(expected).assertIsDisplayed()
                composeRule.onNodeWithText("Close").performClick()
                assertEquals(0, repository.requests.get())
            }
        }
    }

    @Test
    fun closeDuringLoadingCancelsAndFinishesWithoutAStuckSpinner() {
        repository.delayMillis = 30_000
        ActivityScenario.launch<ProcessTextActivity>(processTextIntent("resilient")).use {
            composeRule.waitUntil(5_000) { repository.requests.get() == 1 }
            composeRule.onNodeWithText("Looking up resilient").assertIsDisplayed()
            composeRule.onNodeWithText("Close").performClick()
            composeRule.waitUntil(5_000) { repository.cancelled.get() == 1 }
            assertEquals(1, repository.requests.get())
        }
    }

    private fun processTextIntent(value: String?, readOnly: Boolean = true): Intent {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Intent(context, ProcessTextActivity::class.java).apply {
            action = Intent.ACTION_PROCESS_TEXT
            addCategory(Intent.CATEGORY_DEFAULT)
            type = "text/plain"
            if (value != null) putExtra(Intent.EXTRA_PROCESS_TEXT, value)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, readOnly)
        }
    }

    private class RecordingRepository : DictionaryRepository {
        val requests = AtomicInteger()
        val cancelled = AtomicInteger()
        var delayMillis: Long = 0

        override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> {
            requests.incrementAndGet()
            try {
                if (delayMillis > 0) delay(delayMillis)
            } finally {
                if (delayMillis > 0) cancelled.incrementAndGet()
            }
            return LookupResult.Success(
                listOf(
                    DictionaryEntry(
                        headword = query,
                        functionalLabel = "adjective",
                        pronunciations = listOf(Pronunciation("/rɪˈzɪliənt/", "resili02")),
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
        }

        override suspend fun lookupThesaurus(query: String) = LookupResult.NoMatch
    }
}
