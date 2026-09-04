package io.github.jukerupup.mydictionary.data.repository

import io.github.jukerupup.mydictionary.data.parser.ParseResult
import io.github.jukerupup.mydictionary.data.parser.WiktionaryParser
import io.github.jukerupup.mydictionary.data.remote.WiktionaryApi
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.Pronunciation
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry
import io.github.jukerupup.mydictionary.domain.repository.DictionaryError
import io.github.jukerupup.mydictionary.domain.repository.DictionaryRepository
import io.github.jukerupup.mydictionary.domain.repository.LookupResult
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Dictionary backed by Wiktionary — free, no key, no rate limit.
 *
 * - English definitions come from `en.wiktionary.org` REST API.
 * - Chinese translations + pronunciation audio come from `zh.wiktionary.org`
 *   wikitext (the Chinese page carries both `# 中文釋義` and `{{audio|...}}`).
 *
 * Thesaurus is not a separate Wiktionary concept, so [lookupThesaurus] returns
 * [LookupResult.NoMatch] — the learner thesaurus remains a Merriam-Webster-only
 * feature and should be provided by a composing repository if needed.
 */
class WiktionaryDictionaryRepository(
    private val englishApi: WiktionaryApi,
    private val chineseApi: WiktionaryApi,
    private val parser: WiktionaryParser,
) : DictionaryRepository {

    override suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>> = try {
        val english = englishApi.englishDefinition(query)
        if (!english.isSuccessful) return statusFailure(english.code())
        val englishSource = english.body()?.string()
            ?: return LookupResult.Failure(DictionaryError.MalformedContent("EmptyBody"))

        val parsed = when (val result = parser.parseEnglish(englishSource)) {
            is ParseResult.Failure -> return LookupResult.Failure(
                DictionaryError.MalformedContent(result.error.name),
            )
            is ParseResult.Success -> result.value
        }

        if (parsed.entries.isEmpty()) return LookupResult.NoMatch

        // Fetch Chinese translations + audio in a separate call; degrade gracefully.
        val chinese = runCatching {
            val response = chineseApi.chineseWikitext(page = query)
            if (response.isSuccessful) {
                response.body()?.string()?.let(parser::parseChinese)
            } else {
                null
            }
        }.getOrNull()

        val entries = parsed.entries.map { entry ->
            entry.copy(
                headword = query,
                translations = chinese?.translations ?: emptyList(),
                pronunciations = chinese?.audioReference?.let {
                    listOf(Pronunciation(ipa = null, audioReference = it))
                } ?: emptyList(),
            )
        }
        LookupResult.Success(entries)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SocketTimeoutException) {
        LookupResult.Failure(DictionaryError.Timeout)
    } catch (_: IOException) {
        LookupResult.Failure(DictionaryError.Offline)
    }

    override suspend fun lookupThesaurus(query: String): LookupResult<List<ThesaurusEntry>> =
        LookupResult.NoMatch

    private fun statusFailure(statusCode: Int): LookupResult<List<DictionaryEntry>> = when (statusCode) {
        404 -> LookupResult.NoMatch
        429 -> LookupResult.Failure(DictionaryError.QuotaExceeded)
        else -> LookupResult.Failure(DictionaryError.Server(statusCode))
    }
}
