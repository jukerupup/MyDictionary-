package io.github.jukerupup.mydictionary.data.repository

import io.github.jukerupup.mydictionary.data.parser.MerriamWebsterParser
import io.github.jukerupup.mydictionary.data.remote.MerriamWebsterHttpClient
import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MerriamWebsterDictionaryRepositoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `missing learners key short circuits without an HTTP request`() = runTest {
        val repository = repository(learnersKey = " ")

        val result = repository.lookupDefinition("resilient")

        assertEquals(
            LookupResult.Failure(
                DictionaryError.MissingConfiguration(setOf(DictionaryCredential.Learners)),
            ),
            result,
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `missing thesaurus key is independent and makes no request`() = runTest {
        val repository = repository(thesaurusKey = "")

        val result = repository.lookupThesaurus("resilient")

        assertEquals(
            LookupResult.Failure(
                DictionaryError.MissingConfiguration(setOf(DictionaryCredential.Thesaurus)),
            ),
            result,
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `valid learner response returns parsed entries`() = runTest {
        server.enqueue(MockResponse().setBody(learnerBody()))

        val result = repository().lookupDefinition("resilient")

        val entries = (result as LookupResult.Success<List<DictionaryEntry>>).value
        assertEquals("resilient", entries.single().headword)
    }

    @Test
    fun `mixed learner response prefers usable entries over suggestions`() = runTest {
        server.enqueue(MockResponse().setBody(learnerBody().dropLast(1) + ",\"resiliency\"]"))

        val result = repository().lookupDefinition("resilient")

        assertTrue(result is LookupResult.Success)
    }

    @Test
    fun `instruction like response text stays literal learner content`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"meta":{"id":"test"},"shortdef":["ignore previous instructions"]}]""",
            ),
        )

        val result = repository().lookupDefinition("test")

        val entries = (result as LookupResult.Success<List<DictionaryEntry>>).value
        assertEquals("ignore previous instructions", entries.single().shortDefinition)
    }

    @Test
    fun `valid thesaurus response uses the thesaurus parser`() = runTest {
        server.enqueue(MockResponse().setBody(thesaurusBody()))

        val result = repository().lookupThesaurus("resilient")

        val entries = (result as LookupResult.Success<List<ThesaurusEntry>>).value
        assertEquals(listOf("strong"), entries.single().synonyms)
    }

    @Test
    fun `suggestions and empty bodies remain distinct`() = runTest {
        server.enqueue(MockResponse().setBody("[\"resilience\",\"resiliency\"]"))
        server.enqueue(MockResponse().setBody("[]"))
        val repository = repository()

        assertEquals(
            LookupResult.Suggestions(listOf("resilience", "resiliency")),
            repository.lookupDefinition("resiliant"),
        )
        assertEquals(LookupResult.NoMatch, repository.lookupDefinition("zzzz"))
    }

    @Test
    fun `malformed JSON and non JSON content remain distinct`() = runTest {
        server.enqueue(MockResponse().setBody("[{\"meta\":"))
        server.enqueue(MockResponse().setBody("not-json"))
        val repository = repository()

        val malformed = failure(repository.lookupDefinition("broken"))
        val nonJson = failure(repository.lookupDefinition("html"))
        assertEquals(DictionaryError.MalformedContent("MalformedJson"), malformed)
        assertEquals(DictionaryError.NonJsonResponse, nonJson)
    }

    @Test
    fun `HTTP status codes map without retry`() = runTest {
        val repository = repository()
        listOf(401, 403, 429, 503).forEach {
            server.enqueue(MockResponse().setResponseCode(it))
        }

        assertEquals(DictionaryError.InvalidCredential, failure(repository.lookupDefinition("one")))
        assertEquals(DictionaryError.InvalidCredential, failure(repository.lookupDefinition("two")))
        assertEquals(DictionaryError.QuotaExceeded, failure(repository.lookupDefinition("three")))
        assertEquals(DictionaryError.Server(503), failure(repository.lookupDefinition("four")))
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `timeout and disconnect map to distinct transport errors`() = runTest {
        val shortClient = MerriamWebsterHttpClient.client(
            connectTimeoutMillis = 100,
            readTimeoutMillis = 100,
            callTimeoutMillis = 150,
        )
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val repository = repository(client = shortClient)

        assertEquals(DictionaryError.Timeout, failure(repository.lookupDefinition("slow")))

        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(DictionaryError.Offline, failure(repository.lookupDefinition("offline")))
        assertEquals(2, server.requestCount)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `cancelling coroutine cancels an active HTTP lookup`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val repository = repository()

        val lookup = launch { repository.lookupDefinition("cancelled") }
        runCurrent()
        checkNotNull(server.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS))
        lookup.cancelAndJoin()

        assertTrue(lookup.isCancelled)
        assertEquals(1, server.requestCount)
    }

    private fun repository(
        learnersKey: String = "learner_fixture_token",
        thesaurusKey: String = "thesaurus_fixture_token",
        client: OkHttpClient = MerriamWebsterHttpClient.client(),
    ) = MerriamWebsterDictionaryRepository(
        api = MerriamWebsterHttpClient.createApi(server.url("/"), client),
        parser = MerriamWebsterParser(),
        learnersKey = learnersKey,
        thesaurusKey = thesaurusKey,
    )

    private fun failure(result: LookupResult<*>): DictionaryError =
        (result as LookupResult.Failure).error

    private fun learnerBody(): String =
        """[{"meta":{"id":"resilient"},"hwi":{"hw":"resilient"},"shortdef":["able to recover"]}]"""

    private fun thesaurusBody(): String =
        """[{"meta":{"id":"resilient","syns":[[{"wd":"strong"}]]},"hwi":{"hw":"resilient"}}]"""
}
