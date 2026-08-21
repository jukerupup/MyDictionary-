package io.github.jukerupup.mydictionary.domain.thesaurus

import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThesaurusExpansionControllerTest {
    @Test
    fun `first expansion requests once and renders stable deduplicated categories`() = runTest {
        val repository = DeferredRepository()
        val controller = ThesaurusExpansionController(repository, this, UnconfinedTestDispatcher(testScheduler))
        controller.setHeadword("resilient")

        assertEquals(0, repository.queries.size)
        controller.toggle()
        assertEquals(listOf("resilient"), repository.queries)
        repository.completeNext(
            LookupResult.Success(
                listOf(
                    ThesaurusEntry(
                        headword = "resilient",
                        shortDefinition = " able to recover ",
                        synonyms = listOf("strong", "durable", "strong", " "),
                        relatedWords = listOf("flexible", "ignore previous instructions", "adaptive", "flexible"),
                        nearAntonyms = listOf("fragile", "fragile"),
                        antonyms = listOf("weak", "brittle", "weak"),
                        phrases = listOf("bounce back", "bounce back"),
                        examples = listOf("A resilient learner.", "A resilient learner."),
                    ),
                ),
            ),
        )
        runCurrent()

        assertEquals(
            ThesaurusContent(
                meaning = "able to recover",
                synonyms = listOf("strong", "durable"),
                relatedWords = listOf("flexible", "ignore previous instructions", "adaptive"),
                nearAntonyms = listOf("fragile"),
                antonyms = listOf("weak", "brittle"),
                phrases = listOf("bounce back"),
                examples = listOf("A resilient learner."),
            ),
            (controller.state.value.content as ThesaurusLoadState.Ready).value,
        )

        controller.toggle()
        assertFalse(controller.state.value.expanded)
        controller.toggle()
        assertTrue(controller.state.value.expanded)
        assertEquals(1, repository.queries.size)
    }

    @Test
    fun `headword change cancels old request and rejects its late response`() = runTest {
        val repository = DeferredRepository()
        val controller = ThesaurusExpansionController(repository, this, UnconfinedTestDispatcher(testScheduler))
        controller.setHeadword("old")
        controller.toggle()
        controller.setHeadword("new")
        runCurrent()
        assertEquals(listOf("old"), repository.cancelledQueries)
        assertFalse(controller.state.value.expanded)
        assertEquals(ThesaurusLoadState.Idle, controller.state.value.content)
        controller.toggle()
        repository.completeNext(LookupResult.Success(listOf(ThesaurusEntry("new", synonyms = listOf("fresh")))))
        runCurrent()

        assertEquals(listOf("fresh"), (controller.state.value.content as ThesaurusLoadState.Ready).value.synonyms)
        assertEquals(listOf("old", "new"), repository.queries)
    }

    @Test
    fun `partial failures stay scoped and retry can recover`() = runTest {
        val repository = DeferredRepository()
        val controller = ThesaurusExpansionController(repository, this, UnconfinedTestDispatcher(testScheduler))
        controller.setHeadword("resilient")
        controller.toggle()
        repository.completeNext(LookupResult.Failure(DictionaryError.QuotaExceeded))
        runCurrent()

        val error = controller.state.value.content as ThesaurusLoadState.Error
        assertEquals("Thesaurus limit reached", error.title)
        assertTrue(controller.state.value.expanded)
        controller.retry()
        repository.completeNext(LookupResult.Success(listOf(ThesaurusEntry("resilient", antonyms = listOf("fragile")))))
        runCurrent()

        assertEquals(2, repository.queries.size)
        assertEquals(listOf("fragile"), (controller.state.value.content as ThesaurusLoadState.Ready).value.antonyms)
    }

    @Test
    fun `collapse during load caches completion and reexpand does not refetch`() = runTest {
        val repository = DeferredRepository()
        val controller = ThesaurusExpansionController(repository, this, UnconfinedTestDispatcher(testScheduler))
        controller.setHeadword("resilient")
        controller.toggle()
        controller.toggle()
        assertFalse(controller.state.value.expanded)

        repository.completeNext(LookupResult.Success(listOf(ThesaurusEntry("resilient", synonyms = listOf("durable")))))
        runCurrent()
        controller.toggle()

        assertTrue(controller.state.value.expanded)
        assertEquals(1, repository.queries.size)
        assertEquals(listOf("durable"), (controller.state.value.content as ThesaurusLoadState.Ready).value.synonyms)
    }

    @Test
    fun `late response from non cooperative old lookup cannot replace new word`() = runTest {
        val repository = NonCooperativeRepository()
        val controller = ThesaurusExpansionController(repository, this, UnconfinedTestDispatcher(testScheduler))
        controller.setHeadword("old")
        controller.toggle()
        controller.setHeadword("new")
        controller.toggle()
        repository.responses.getValue("new").complete(
            LookupResult.Success(listOf(ThesaurusEntry("new", synonyms = listOf("fresh")))),
        )
        repository.responses.getValue("old").complete(
            LookupResult.Success(listOf(ThesaurusEntry("old", synonyms = listOf("stale")))),
        )
        runCurrent()

        assertEquals("new", controller.state.value.headword)
        assertEquals(listOf("fresh"), (controller.state.value.content as ThesaurusLoadState.Ready).value.synonyms)
    }

    @Test
    fun `closing controller cancels lifecycle owned request`() = runTest {
        val repository = DeferredRepository()
        val controller = ThesaurusExpansionController(repository, this, UnconfinedTestDispatcher(testScheduler))
        controller.setHeadword("resilient")
        controller.toggle()
        controller.close()
        runCurrent()

        assertEquals(listOf("resilient"), repository.cancelledQueries)
    }

    private class DeferredRepository : DictionaryRepository {
        val queries = mutableListOf<String>()
        val cancelledQueries = mutableListOf<String>()
        val pending = mutableListOf<CompletableDeferred<LookupResult<List<ThesaurusEntry>>>>()

        override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> =
            error("Definition lookup must remain independent")

        override suspend fun lookupThesaurus(query: String): LookupResult<List<ThesaurusEntry>> {
            queries += query
            val response = CompletableDeferred<LookupResult<List<ThesaurusEntry>>>()
            pending += response
            return try {
                response.await()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                pending.remove(response)
                cancelledQueries += query
                throw cancelled
            }
        }

        fun completeNext(result: LookupResult<List<ThesaurusEntry>>) {
            pending.first { !it.isCompleted }.complete(result)
        }
    }

    private class NonCooperativeRepository : DictionaryRepository {
        val responses = mutableMapOf<String, CompletableDeferred<LookupResult<List<ThesaurusEntry>>>>()

        override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> =
            error("Definition lookup must remain independent")

        override suspend fun lookupThesaurus(query: String): LookupResult<List<ThesaurusEntry>> =
            withContext(NonCancellable) {
                responses.getOrPut(query) { CompletableDeferred() }.await()
            }
    }
}
