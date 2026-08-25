package io.github.jukerupup.mydictionary.bubble

import io.github.jukerupup.mydictionary.domain.lookup.LookupState
import io.github.jukerupup.mydictionary.domain.model.Definition
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BubbleControllerTest {
    @Test
    fun `lookup reaches content state through the shared engine`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = BubbleController(
            repository = RecordingRepository(),
            scope = this,
            dispatcher = dispatcher,
        )

        controller.lookup("resilient")
        advanceUntilIdle()

        val state = controller.state.value
        assertTrue(state is LookupState.Content)
        assertEquals("resilient", (state as LookupState.Content).query)
        controller.close()
    }

    @Test
    fun `invalid input maps to typed invalid input state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = BubbleController(
            repository = RecordingRepository(),
            scope = this,
            dispatcher = dispatcher,
        )

        controller.lookup("   ")
        advanceUntilIdle()

        assertTrue(controller.state.value is LookupState.InvalidInput)
        controller.close()
    }

    @Test
    fun `cancel returns the bubble to idle`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = BubbleController(
            repository = RecordingRepository(),
            scope = this,
            dispatcher = dispatcher,
        )

        controller.lookup("resilient")
        advanceUntilIdle()
        controller.cancel()

        assertEquals(LookupState.Idle, controller.state.value)
        controller.close()
    }

    private class RecordingRepository : DictionaryRepository {
        override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> =
            LookupResult.Success(
                listOf(
                    DictionaryEntry(
                        headword = query,
                        functionalLabel = "adjective",
                        pronunciations = emptyList(),
                        definitions = listOf(Definition("able to become strong after difficulty")),
                        offensive = false,
                    ),
                ),
            )

        override suspend fun lookupThesaurus(query: String) = LookupResult.NoMatch
    }
}
