package io.github.jukerupup.mydictionary.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MerriamWebsterApi {
    @GET("api/v3/references/learners/json/{word}")
    suspend fun learners(
        @Path("word") word: String,
        @Query("key") key: String,
    ): Response<ResponseBody>

    @GET("api/v3/references/ithesaurus/json/{word}")
    suspend fun thesaurus(
        @Path("word") word: String,
        @Query("key") key: String,
    ): Response<ResponseBody>
}

object MerriamWebsterHttpClient {
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
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun createApi(
        baseUrl: HttpUrl,
        client: OkHttpClient = client(),
    ): MerriamWebsterApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .build()
        .create(MerriamWebsterApi::class.java)
}
