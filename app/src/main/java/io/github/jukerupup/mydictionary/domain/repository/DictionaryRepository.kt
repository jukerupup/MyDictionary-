package io.github.jukerupup.mydictionary.domain.repository

import io.github.jukerupup.mydictionary.domain.model.DictionaryCredential
import io.github.jukerupup.mydictionary.domain.model.DictionaryEntry
import io.github.jukerupup.mydictionary.domain.model.ThesaurusEntry

interface DictionaryRepository {
    suspend fun lookupDefinition(query: String): LookupResult<List<DictionaryEntry>>

    suspend fun lookupThesaurus(query: String): LookupResult<List<ThesaurusEntry>>
}

sealed interface LookupResult<out T> {
    data class Success<T>(val value: T) : LookupResult<T>
    data class Suggestions(val values: List<String>) : LookupResult<Nothing>
    data object NoMatch : LookupResult<Nothing>
    data class Failure(val error: DictionaryError) : LookupResult<Nothing>
}

sealed interface DictionaryError {
    data class MissingConfiguration(
        val credentials: Set<DictionaryCredential>,
    ) : DictionaryError

    data object InvalidCredential : DictionaryError
    data object QuotaExceeded : DictionaryError
    data object Offline : DictionaryError
    data object Timeout : DictionaryError
    data object NonJsonResponse : DictionaryError
    data class Server(val statusCode: Int?) : DictionaryError
    data class MalformedContent(val reason: String) : DictionaryError
}
