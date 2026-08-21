package io.github.jukerupup.mydictionary.data.repository

import io.github.jukerupup.mydictionary.data.parser.DictionaryPayload
import io.github.jukerupup.mydictionary.data.parser.MerriamWebsterParser
import io.github.jukerupup.mydictionary.data.parser.ParseResult
import io.github.jukerupup.mydictionary.data.remote.MerriamWebsterApi
import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Response

class MerriamWebsterDictionaryRepository(
    private val api: MerriamWebsterApi,
    private val parser: MerriamWebsterParser,
    learnersKey: String,
    thesaurusKey: String,
) : DictionaryRepository {
    private val learnersKey = learnersKey.trim()
    private val thesaurusKey = thesaurusKey.trim()

    override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> {
        if (learnersKey.isBlank()) return missing(DictionaryCredential.Learners)
        return request(
            call = { api.learners(query, learnersKey) },
            parse = parser::parseLearners,
        )
    }

    override suspend fun lookupThesaurus(query: String): LookupResult<List<ThesaurusEntry>> {
        if (thesaurusKey.isBlank()) return missing(DictionaryCredential.Thesaurus)
        return request(
            call = { api.thesaurus(query, thesaurusKey) },
            parse = parser::parseThesaurus,
        )
    }

    private suspend fun <T> request(
        call: suspend () -> Response<ResponseBody>,
        parse: (String) -> ParseResult<DictionaryPayload<T>>,
    ): LookupResult<List<T>> = try {
        val response = call()
        if (!response.isSuccessful) return statusFailure(response.code())
        val source = response.body()?.string()
            ?: return LookupResult.Failure(DictionaryError.MalformedContent("EmptyBody"))
        if (!source.isJsonContainerShaped()) {
            return LookupResult.Failure(DictionaryError.NonJsonResponse)
        }
        when (val result = parse(source)) {
            is ParseResult.Failure -> LookupResult.Failure(
                DictionaryError.MalformedContent(result.error.name),
            )
            is ParseResult.Success -> result.value.toLookupResult()
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SocketTimeoutException) {
        LookupResult.Failure(DictionaryError.Timeout)
    } catch (_: IOException) {
        LookupResult.Failure(DictionaryError.Offline)
    }

    private fun statusFailure(statusCode: Int): LookupResult.Failure = when (statusCode) {
        401, 403 -> LookupResult.Failure(DictionaryError.InvalidCredential)
        429 -> LookupResult.Failure(DictionaryError.QuotaExceeded)
        else -> LookupResult.Failure(DictionaryError.Server(statusCode))
    }

    private fun <T> DictionaryPayload<T>.toLookupResult(): LookupResult<List<T>> = when {
        entries.isNotEmpty() -> LookupResult.Success(entries)
        suggestions.isNotEmpty() -> LookupResult.Suggestions(suggestions)
        else -> LookupResult.NoMatch
    }

    private fun missing(credential: DictionaryCredential) = LookupResult.Failure(
        DictionaryError.MissingConfiguration(setOf(credential)),
    )
}

private fun String.isJsonContainerShaped(): Boolean =
    firstOrNull { !it.isWhitespace() } == '[' || firstOrNull { !it.isWhitespace() } == '{'
