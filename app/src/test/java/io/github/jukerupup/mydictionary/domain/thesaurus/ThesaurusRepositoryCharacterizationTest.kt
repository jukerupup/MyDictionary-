package io.github.jukerupup.mydictionary.domain.thesaurus

import io.github.jukerupup.mydictionary.data.parser.MerriamWebsterParser
import io.github.jukerupup.mydictionary.data.remote.MerriamWebsterHttpClient
import io.github.jukerupup.mydictionary.data.repository.MerriamWebsterDictionaryRepository
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class ThesaurusRepositoryCharacterizationTest {
    @Test
    fun `lookupThesaurus preserves its exact query and parsed result`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse().setBody(
                    """[{"meta":{"id":"resilient","syns":[[{"wd":"strong"}]]},"hwi":{"hw":"resilient"},"shortdef":["able to recover"]}]""",
                ),
            )
            val repository = MerriamWebsterDictionaryRepository(
                api = MerriamWebsterHttpClient.createApi(server.url("/")),
                parser = MerriamWebsterParser(),
                learnersKey = "learner_fixture_token",
                thesaurusKey = "thesaurus_fixture_token",
            )

            val result = repository.lookupThesaurus("resilient")

            assertEquals(
                LookupResult.Success(
                    listOf(
                        ThesaurusEntry(
                            headword = "resilient",
                            shortDefinition = "able to recover",
                            synonyms = listOf("strong"),
                        ),
                    ),
                ),
                result,
            )
            assertEquals("/api/v3/references/ithesaurus/json/resilient?key=thesaurus_fixture_token", server.takeRequest().path)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }
}
