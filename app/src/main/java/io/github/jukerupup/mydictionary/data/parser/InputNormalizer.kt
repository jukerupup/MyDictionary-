package io.github.jukerupup.mydictionary.data.parser

sealed interface NormalizationResult {
    data class Valid(val value: String) : NormalizationResult
    data class Invalid(val error: InputError) : NormalizationResult
}

enum class InputError {
    Blank,
    TooLong,
}

object InputNormalizer {
    private const val MAX_CODE_POINTS = 80

    fun normalize(input: CharSequence?): NormalizationResult {
        if (input == null) return NormalizationResult.Invalid(InputError.Blank)
        var value = trimUnicodeWhitespace(input.toString())
        value = stripEdgePunctuation(value)
        value = trimUnicodeWhitespace(value)
        if (value.isEmpty()) return NormalizationResult.Invalid(InputError.Blank)
        if (value.codePointCount(0, value.length) > MAX_CODE_POINTS) {
            return NormalizationResult.Invalid(InputError.TooLong)
        }
        return NormalizationResult.Valid(value)
    }

    private fun trimUnicodeWhitespace(value: String): String {
        var start = 0
        var end = value.length
        while (start < end) {
            val point = value.codePointAt(start)
            if (!isWhitespace(point)) break
            start += Character.charCount(point)
        }
        while (start < end) {
            val point = value.codePointBefore(end)
            if (!isWhitespace(point)) break
            end -= Character.charCount(point)
        }
        return value.substring(start, end)
    }

    private fun stripEdgePunctuation(value: String): String {
        var start = 0
        var end = value.length
        while (start < end) {
            val point = value.codePointAt(start)
            if (!isPunctuation(point)) break
            start += Character.charCount(point)
        }
        while (start < end) {
            val point = value.codePointBefore(end)
            if (!isPunctuation(point)) break
            end -= Character.charCount(point)
        }
        return value.substring(start, end)
    }

    private fun isWhitespace(codePoint: Int): Boolean =
        Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)

    private fun isPunctuation(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(),
        -> true
        else -> false
    }
}
