package io.github.jukerupup.mydictionary.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class MerriamWebsterApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: MerriamWebsterApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = MerriamWebsterHttpClient.createApi(server.url("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `learners endpoint records encoded word and learners key on exact path`() = runTest {
        server.enqueue(MockResponse().setBody("[]"))

        api.learners("ice cream", "learner_fixture_token")

        val url = checkNotNull(server.takeRequest().requestUrl)
        assertEquals("/api/v3/references/learners/json/ice%20cream", url.encodedPath)
        assertEquals("learner_fixture_token", url.queryParameter("key"))
    }

    @Test
    fun `thesaurus endpoint records encoded word and separate thesaurus key`() = runTest {
        server.enqueue(MockResponse().setBody("[]"))

        api.thesaurus("well-being", "thesaurus_fixture_token")

        val url = checkNotNull(server.takeRequest().requestUrl)
        assertEquals("/api/v3/references/ithesaurus/json/well-being", url.encodedPath)
        assertEquals("thesaurus_fixture_token", url.queryParameter("key"))
    }

    @Test
    fun `HTTP client has bounded timeouts without retry or redirects`() {
        val client = MerriamWebsterHttpClient.client(
            connectTimeoutMillis = 101,
            readTimeoutMillis = 202,
            callTimeoutMillis = 303,
        )

        assertEquals(101, client.connectTimeoutMillis)
        assertEquals(202, client.readTimeoutMillis)
        assertEquals(303, client.callTimeoutMillis)
        assertFalse(client.retryOnConnectionFailure)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
    }
}
