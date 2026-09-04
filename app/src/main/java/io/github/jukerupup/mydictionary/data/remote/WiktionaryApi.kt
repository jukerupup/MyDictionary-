package io.github.jukerupup.mydictionary.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Wiktionary endpoints.
 *
 * - [englishDefinition] uses the English Wiktionary REST API, which returns
 *   clean structured JSON (no key, no rate limit).
 * - [chineseWikitext] uses the Chinese Wiktionary MediaWiki Action API to fetch
 *   the page wikitext, which contains the Chinese translations (e.g. `# 彈回的`).
 */
interface WiktionaryApi {
    @GET("api/rest_v1/page/definition/{word}")
    suspend fun englishDefinition(@Path("word") word: String): Response<ResponseBody>

    @GET("w/api.php")
    suspend fun chineseWikitext(
        @Query("action") action: String = "parse",
        @Query("page") page: String,
        @Query("prop") prop: String = "wikitext",
        @Query("format") format: String = "json",
    ): Response<ResponseBody>
}

object WiktionaryHttpClient {
    private const val DEFAULT_TIMEOUT_MILLIS = 10_000L

    fun client(
        connectTimeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        readTimeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        callTimeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
        .callTimeout(callTimeoutMillis, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .followRedirects(true)
        .build()

    fun createEnglishApi(
        httpClient: OkHttpClient = client(),
    ): WiktionaryApi = Retrofit.Builder()
        .baseUrl("https://en.wiktionary.org/".toHttpUrl())
        .client(httpClient)
        .build()
        .create(WiktionaryApi::class.java)

    fun createChineseApi(
        httpClient: OkHttpClient = client(),
    ): WiktionaryApi = Retrofit.Builder()
        .baseUrl("https://zh.wiktionary.org/".toHttpUrl())
        .client(httpClient)
        .build()
        .create(WiktionaryApi::class.java)
}
