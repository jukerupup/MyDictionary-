package io.github.jukerupup.mydictionary.data.parser

data class DictionaryPayload<T>(
    val entries: List<T>,
    val suggestions: List<String>,
)

sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>
    data class Failure(val error: ParseFailure) : ParseResult<Nothing>
}

enum class ParseFailure {
    MalformedJson,
    MalformedMarkup,
}
