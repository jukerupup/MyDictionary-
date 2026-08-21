package io.github.jukerupup.mydictionary.domain.lookup

import io.github.jukerupup.mydictionary.domain.audio.AudioPlaybackState
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LookupStateEngineTest {
    @Test
    fun `duplicate in flight query is coalesced`() = runTest {
        val repository = ControlledRepository()
        val engine = engine(repository)

        engine.lookup("resilient")
        engine.lookup("resilient")
        repository.complete("resilient", LookupResult.Success(listOf(entry("resilient"))))
        advanceUntilIdle()

        assertEquals(listOf("resilient"), repository.definitionQueries)
    }

    @Test
    fun `rapid A to B keeps B final even if A ignores cancellation`() = runTest {
        val repository = ControlledRepository(ignoreCancellationFor = "alpha")
        val engine = engine(repository)

        engine.lookup("alpha")
        advanceUntilIdle()
        engine.lookup("beta")
        repository.complete("beta", LookupResult.Success(listOf(entry("beta"))))
        advanceUntilIdle()
        repository.complete("alpha", LookupResult.Success(listOf(entry("alpha"))))
        advanceUntilIdle()

        assertEquals("beta", (engine.state.value as LookupState.Content).query)
    }

    @Test
    fun `invalid latest input cannot be replaced by a cancelled stale result`() = runTest {
        val repository = ControlledRepository(ignoreCancellationFor = "alpha")
        val engine = engine(repository)
        engine.lookup("alpha")
        advanceUntilIdle()

        engine.lookup("   ")
        repository.complete("alpha", LookupResult.Success(listOf(entry("alpha"))))
        advanceUntilIdle()

        assertEquals(LookupState.InvalidInput(io.github.jukerupup.mydictionary.data.parser.InputError.Blank), engine.state.value)
    }

    @Test
    fun `thesaurus is on demand and failure preserves definition`() = runTest {
        val repository = ControlledRepository()
        val engine = engine(repository)
        engine.lookup("resilient")
        repository.complete("resilient", LookupResult.Success(listOf(entry("resilient"))))
        advanceUntilIdle()

        assertTrue(repository.thesaurusQueries.isEmpty())
        engine.loadThesaurus()
        repository.completeThesaurus(
            "resilient",
            LookupResult.Failure(DictionaryError.QuotaExceeded),
        )
        advanceUntilIdle()

        val content = engine.state.value as LookupState.Content
        assertEquals("resilient", content.entries.single().headword)
        assertEquals(
            ThesaurusLoadState.Failure(DictionaryError.QuotaExceeded),
            content.thesaurus,
        )
    }

    @Test
    fun `audio failure is independent from successful definition`() = runTest {
        val repository = ControlledRepository()
        val engine = engine(repository)
        engine.lookup("resilient")
        repository.complete("resilient", LookupResult.Success(listOf(entry("resilient"))))
        advanceUntilIdle()

        engine.updateAudioState(AudioPlaybackState.Error("unavailable"))

        val content = engine.state.value as LookupState.Content
        assertEquals("resilient", content.entries.single().headword)
        assertEquals(AudioPlaybackState.Error("unavailable"), content.audio)
    }

    @Test
    fun `suggestion no match and failure results map to separate states`() = runTest {
        val repository = ControlledRepository()
        val engine = engine(repository)
        val cases: List<Pair<String, Pair<LookupResult<List<DictionaryEntry>>, LookupState>>> = listOf(
            "suggest" to (LookupResult.Suggestions(listOf("colour")) to
                LookupState.Suggestions("suggest", listOf("colour"))),
            "missing" to (LookupResult.NoMatch to LookupState.NoMatch("missing")),
            "offline" to (LookupResult.Failure(DictionaryError.Offline) to
                LookupState.Failure("offline", DictionaryError.Offline)),
        )

        cases.forEach { (query, pair) ->
            val (result, expected) = pair
            engine.lookup(query)
            repository.complete(query, result)
            advanceUntilIdle()
            assertEquals(expected, engine.state.value)
        }
    }

    private fun TestScope.engine(repository: DictionaryRepository) =
        LookupStateEngine(repository, this, StandardTestDispatcher(testScheduler))

    private fun entry(word: String) = DictionaryEntry(word, null, emptyList(), emptyList(), false)
}

private class ControlledRepository(
    private val ignoreCancellationFor: String? = null,
) : DictionaryRepository {
    val definitionQueries = mutableListOf<String>()
    val thesaurusQueries = mutableListOf<String>()
    private val definitions = mutableMapOf<String, CompletableDeferred<LookupResult<List<DictionaryEntry>>>>()
    private val thesaurus = mutableMapOf<String, CompletableDeferred<LookupResult<List<ThesaurusEntry>>>>()

    override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> {
        definitionQueries += query
        val result = definitions.getOrPut(query, ::CompletableDeferred)
        return if (query == ignoreCancellationFor) withContext(NonCancellable) { result.await() } else result.await()
    }

    override suspend fun lookupThesaurus(query: String): LookupResult<List<ThesaurusEntry>> {
        thesaurusQueries += query
        return thesaurus.getOrPut(query, ::CompletableDeferred).await()
    }

    fun complete(query: String, result: LookupResult<List<DictionaryEntry>>) {
        definitions.getOrPut(query, ::CompletableDeferred).complete(result)
    }

    fun completeThesaurus(query: String, result: LookupResult<List<ThesaurusEntry>>) {
        thesaurus.getOrPut(query, ::CompletableDeferred).complete(result)
    }
}
