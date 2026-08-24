package io.github.jukerupup.mydictionary.ui.thesaurus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import io.github.jukerupup.mydictionary.domain.thesaurus.ThesaurusExpansionController
import io.github.jukerupup.mydictionary.ui.theme.DictionarySpacing
import io.github.jukerupup.mydictionary.ui.theme.MyDictionaryTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThesaurusSectionInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var qaScrollState: ScrollState

    @Test
    fun collapsedExpandCategoriesCollapseAndReexpandUseOneRequest() {
        val repository = ScriptedRepository(
            LookupResult.Success(
                listOf(
                    ThesaurusEntry(
                        headword = "resilient",
                        shortDefinition = "able to recover after difficulty",
                        synonyms = listOf("strong", "durable", "strong"),
                        relatedWords = listOf("flexible", "adaptive"),
                        nearAntonyms = listOf("fragile"),
                        antonyms = listOf("weak", "brittle"),
                        phrases = listOf("bounce back"),
                        examples = listOf("A resilient learner tries again."),
                    ),
                ),
            ),
        )
        show(repository)

        assertEquals(0, repository.requestCount)
        composeRule.onAllNodesWithTag("thesaurus_result").assertCountEquals(0)
        composeRule.onNodeWithTag("thesaurus_toggle").performClick()
        composeRule.waitForIdle()
        assertEquals(1, repository.requestCount)
        composeRule.onNodeWithText(PRIMARY_DEFINITION).performScrollTo().assertIsDisplayed()
        val headings = listOf(
            "Shared meaning", "Synonyms", "Related words", "Near-antonyms", "Antonyms", "Phrases", "Examples",
        )
        headings.forEach { heading ->
            composeRule.onNodeWithText(heading).assertIsDisplayed()
        }
        capture("thesaurus-categories-all.png")

        composeRule.onNodeWithTag("thesaurus_toggle").performScrollTo().performClick()
        composeRule.onAllNodesWithTag("thesaurus_result").assertCountEquals(0)
        composeRule.onNodeWithTag("thesaurus_toggle").performClick()
        composeRule.waitForIdle()
        assertEquals(1, repository.requestCount)
        composeRule.onNodeWithTag("thesaurus_result").assertIsDisplayed()
        record("collapsed_expand_trace.txt", "before=0\nafterFirstExpand=1\nafterReexpand=1\ncategories=all\n")
    }

    @Test
    fun quotaStaysScopedAndRetryRecovers() {
        verifyFailureAndRetry("quota", LookupResult.Failure(DictionaryError.QuotaExceeded))
    }

    @Test
    fun malformedContentStaysScopedAndRetryRecovers() {
        verifyFailureAndRetry(
            "malformed",
            LookupResult.Failure(DictionaryError.MalformedContent("MalformedJson")),
        )
    }

    @Test
    fun noMatchStaysScopedAndRetryRecovers() {
        verifyFailureAndRetry("no-match", LookupResult.NoMatch)
    }

    private fun verifyFailureAndRetry(
        label: String,
        failure: LookupResult<List<ThesaurusEntry>>,
    ) {
        val repository = ScriptedRepository(
            failure,
            LookupResult.Success(listOf(ThesaurusEntry("resilient", synonyms = listOf("durable")))),
        )
        show(repository)
        composeRule.onNodeWithTag("thesaurus_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(PRIMARY_DEFINITION).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("thesaurus_error").assertIsDisplayed()
        resetScrollAndAssertFailureBounds()
        capture("thesaurus-failure-$label.png")
        composeRule.onNodeWithText("Try again").performClick()
        composeRule.waitForIdle()
        assertEquals(2, repository.requestCount)
        composeRule.onNodeWithText("durable").performScrollTo().assertIsDisplayed()
        capture("thesaurus-recovered-$label.png")
        record("partial_failure_$label.txt", "failure=$label\nretry=recovered\nprimaryDefinition=visible\n")
    }

    private fun show(repository: ScriptedRepository) {
        composeRule.setContent {
            MyDictionaryTheme {
                val scope = rememberCoroutineScope()
                val controller = remember(repository) {
                    ThesaurusExpansionController(repository, scope).also { it.setHeadword("resilient") }
                }
                val state by controller.state.collectAsState()
                val scrollState = rememberScrollState()
                SideEffect { qaScrollState = scrollState }
                DisposableEffect(controller) { onDispose(controller::close) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(scrollState)
                        .padding(DictionarySpacing.Space4)
                        .testTag("thesaurus_qa_surface"),
                    verticalArrangement = Arrangement.spacedBy(DictionarySpacing.Space6),
                ) {
                    Text(PRIMARY_DEFINITION, modifier = Modifier.testTag("primary_definition_sentinel"))
                    ThesaurusSection(state, controller::toggle, controller::retry)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun capture(name: String) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(evidenceDirectory(), name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun resetScrollAndAssertFailureBounds() {
        composeRule.runOnIdle {
            qaScrollState.dispatchRawDelta(-qaScrollState.value.toFloat())
            assertEquals(0, qaScrollState.value)
        }
        composeRule.waitForIdle()
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        listOf(
            composeRule.onNodeWithText(PRIMARY_DEFINITION),
            composeRule.onNodeWithTag("thesaurus_toggle"),
            composeRule.onNodeWithTag("thesaurus_error"),
            composeRule.onNodeWithText("Try again"),
        ).forEach { node ->
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("Node top $bounds is outside root $root", bounds.top >= root.top)
            assertTrue("Node bottom $bounds is outside root $root", bounds.bottom <= root.bottom)
        }
    }

    private fun record(name: String, contents: String) {
        File(evidenceDirectory(), name).writeText(contents)
    }

    private fun evidenceDirectory(): File = checkNotNull(
        InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("task8-evidence"),
    ).apply { mkdirs() }

    private class ScriptedRepository(vararg results: LookupResult<List<ThesaurusEntry>>) : DictionaryRepository {
        private val results = ArrayDeque(results.toList())
        var requestCount = 0
            private set

        override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> =
            error("Definition lookup must remain usable and independent")

        override suspend fun lookupThesaurus(query: String): LookupResult<List<ThesaurusEntry>> {
            requestCount += 1
            return results.removeFirst()
        }
    }

    private companion object {
        const val PRIMARY_DEFINITION = "Primary definition remains visible and usable."
    }
}
