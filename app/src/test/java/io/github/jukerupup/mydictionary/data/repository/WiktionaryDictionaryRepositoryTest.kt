package io.github.jukerupup.mydictionary.data.repository

import io.github.jukerupup.mydictionary.data.parser.WiktionaryParser
import io.github.jukerupup.mydictionary.data.remote.WiktionaryApi
import io.github.jukerupup.mydictionary.data.remote.WiktionaryHttpClient
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WiktionaryDictionaryRepositoryTest {
    private lateinit var enServer: MockWebServer
    private lateinit var zhServer: MockWebServer
    private lateinit var repository: WiktionaryDictionaryRepository

    @Before
    fun setUp() {
        enServer = MockWebServer()
        zhServer = MockWebServer()
        enServer.start()
        zhServer.start()
        val enApi = retrofit2.Retrofit.Builder()
            .baseUrl(enServer.url("/"))
            .client(WiktionaryHttpClient.client())
            .build()
            .create(WiktionaryApi::class.java)
        val zhApi = retrofit2.Retrofit.Builder()
            .baseUrl(zhServer.url("/"))
            .client(WiktionaryHttpClient.client())
            .build()
            .create(WiktionaryApi::class.java)
        repository = WiktionaryDictionaryRepository(
            englishApi = enApi,
            chineseApi = zhApi,
            parser = WiktionaryParser(),
        )
    }

    @After
    fun tearDown() {
        enServer.shutdown()
        zhServer.shutdown()
    }

    @Test
    fun `merges english definition with chinese translations and audio`() = runTest {
        enServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"en":[{"partOfSpeech":"Adjective","language":"English","definitions":[
                  {"definition":"Returning quickly to original shape after force is applied; elastic."}
                ]}]}
                """.trimIndent(),
            ),
        )
        zhServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"parse":{"wikitext":{"*":"==英語==\n* {{audio|en|en-us-resilient.ogg}}\n#[[彈回的]]\n#[[有彈力的]]"}}}
                """.trimIndent(),
            ),
        )

        val result = repository.lookupDefinition("resilient")

        assertTrue(result is LookupResult.Success)
        val entry = (result as LookupResult.Success).value.single()
        assertEquals("resilient", entry.headword)
        assertEquals("Adjective", entry.functionalLabel)
        assertEquals(listOf("彈回的", "有彈力的"), entry.translations)
        assertEquals("en-us-resilient.ogg", entry.pronunciations.first().audioReference)
    }

    @Test
    fun `english definition succeeds even when chinese lookup fails`() = runTest {
        enServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"en":[{"partOfSpeech":"Noun","language":"English","definitions":[
                  {"definition":"A clear definition."}
                ]}]}
                """.trimIndent(),
            ),
        )
        zhServer.enqueue(MockResponse().setResponseCode(500))

        val result = repository.lookupDefinition("test")

        assertTrue(result is LookupResult.Success)
        val entry = (result as LookupResult.Success).value.single()
        assertEquals(emptyList<String>(), entry.translations)
        assertEquals("A clear definition.", entry.definitions.first().text)
    }

    @Test
    fun `404 returns no match`() = runTest {
        enServer.enqueue(MockResponse().setResponseCode(404))

        val result = repository.lookupDefinition("zzzz")

        assertEquals(LookupResult.NoMatch, result)
    }
}
